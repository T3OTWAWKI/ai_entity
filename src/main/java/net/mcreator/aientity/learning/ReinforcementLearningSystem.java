package net.mcreator.aientity.learning;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.mcreator.aientity.entity.C2Entity;

import java.util.*;

public class ReinforcementLearningSystem extends SavedData {
    private static final String DATA_NAME = "ai_entity_learning";
    
    // Q-Learning parameters
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.95;
    private static final double EXPLORATION_RATE = 0.1;
    private static final double EXPLORATION_DECAY = 0.995;
    
    // State-Action Q-table
    private Map<String, Map<String, Double>> qTable = new HashMap<>();
    
    // Experience buffer for training
    private List<Experience> experienceBuffer = new ArrayList<>();
    private static final int BUFFER_SIZE = 1000;
    
    // Current exploration rate (decreases over time)
    private double currentExplorationRate = EXPLORATION_RATE;
    
    // Performance tracking
    private int totalEncounters = 0;
    private int successfulKills = 0;
    private int playerEscapes = 0;
    
    public ReinforcementLearningSystem() {
        super();
    }
    
    public static ReinforcementLearningSystem get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            ReinforcementLearningSystem::load,
            ReinforcementLearningSystem::new,
            DATA_NAME
        );
    }
    
    public static ReinforcementLearningSystem load(CompoundTag tag) {
        ReinforcementLearningSystem system = new ReinforcementLearningSystem();
        system.loadFromNBT(tag);
        return system;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        return saveToNBT(tag);
    }
    
    // Action enumeration
    public enum Action {
        MOVE_TOWARDS_PLAYER,
        BREAK_BLOCK_DIRECT,
        BREAK_BLOCK_PREDICTIVE,
        ATTACK_MELEE,
        WAIT_AND_OBSERVE,
        FLANK_LEFT,
        FLANK_RIGHT,
        RETREAT_AND_REPOSITION
    }
    
    // Game state representation
    public static class GameState {
        public double distanceToPlayer;
        public boolean hasLineOfSight;
        public int blocksInPath;
        public double playerHealthPercent;
        public boolean playerIsMoving;
        public String playerBiome;
        public int timeOfDay; // 0-3 (morning, day, evening, night)
        public boolean playerInWater;
        public boolean playerOnGround;
        public int playerArmor; // 0-4 scale
        
        public String toStateString() {
            return String.format("%.1f_%b_%d_%.1f_%b_%s_%d_%b_%b_%d",
                Math.round(distanceToPlayer * 2) / 2.0, // Round to 0.5
                hasLineOfSight,
                Math.min(blocksInPath, 10), // Cap at 10
                Math.round(playerHealthPercent * 4) / 4.0, // Round to 0.25
                playerIsMoving,
                playerBiome,
                timeOfDay,
                playerInWater,
                playerOnGround,
                playerArmor
            );
        }
    }
    
    // Experience for training
    public static class Experience {
        public String state;
        public String action;
        public double reward;
        public String nextState;
        public boolean isTerminal;
        
        public Experience(String state, String action, double reward, String nextState, boolean isTerminal) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
            this.isTerminal = isTerminal;
        }
    }
    
    // Get current game state
    public GameState getCurrentState(C2Entity entity, Player player) {
        GameState state = new GameState();
        
        state.distanceToPlayer = entity.distanceTo(player);
        state.hasLineOfSight = entity.hasLineOfSight(player);
        state.blocksInPath = countBlocksInPath(entity, player);
        state.playerHealthPercent = player.getHealth() / player.getMaxHealth();
        state.playerIsMoving = player.getDeltaMovement().lengthSqr() > 0.01;
        state.playerBiome = entity.level().getBiome(player.blockPosition()).toString();
        state.timeOfDay = (int) (entity.level().getDayTime() % 24000 / 6000);
        state.playerInWater = player.isInWater();
        state.playerOnGround = player.onGround();
        state.playerArmor = calculatePlayerArmorLevel(player);
        
        return state;
    }
    
    // Choose action using epsilon-greedy policy
    public Action chooseAction(GameState state) {
        String stateString = state.toStateString();
        
        // Exploration vs exploitation
        if (Math.random() < currentExplorationRate) {
            // Explore: choose random action
            Action[] actions = Action.values();
            return actions[new Random().nextInt(actions.length)];
        } else {
            // Exploit: choose best known action
            return getBestAction(stateString);
        }
    }
    
    // Get best action for a state
    public Action getBestAction(String stateString) {
        Map<String, Double> stateActions = qTable.get(stateString);
        if (stateActions == null || stateActions.isEmpty()) {
            // If no experience, choose random action
            Action[] actions = Action.values();
            return actions[new Random().nextInt(actions.length)];
        }
        
        return Action.valueOf(stateActions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .get().getKey());
    }
    
    // Update Q-table with experience
    public void updateQTable(String state, String action, double reward, String nextState, boolean isTerminal) {
        // Add experience to buffer
        experienceBuffer.add(new Experience(state, action, reward, nextState, isTerminal));
        
        // Keep buffer size manageable
        if (experienceBuffer.size() > BUFFER_SIZE) {
            experienceBuffer.remove(0);
        }
        
        // Q-learning update
        double currentQ = getQValue(state, action);
        double maxNextQ = isTerminal ? 0 : getMaxQValue(nextState);
        double newQ = currentQ + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - currentQ);
        
        setQValue(state, action, newQ);
        
        // Decay exploration rate
        currentExplorationRate = Math.max(0.01, currentExplorationRate * EXPLORATION_DECAY);
        
        setDirty();
    }
    
    // Get Q-value for state-action pair
    private double getQValue(String state, String action) {
        return qTable.computeIfAbsent(state, k -> new HashMap<>()).getOrDefault(action, 0.0);
    }
    
    // Set Q-value for state-action pair
    private void setQValue(String state, String action, double value) {
        qTable.computeIfAbsent(state, k -> new HashMap<>()).put(action, value);
    }
    
    // Get maximum Q-value for a state
    private double getMaxQValue(String state) {
        Map<String, Double> stateActions = qTable.get(state);
        if (stateActions == null || stateActions.isEmpty()) {
            return 0.0;
        }
        return Collections.max(stateActions.values());
    }
    
    // Reward calculation
    public double calculateReward(C2Entity entity, Player player, Action action, GameState prevState, GameState currentState) {
        double reward = 0.0;
        
        // Base rewards
        if (currentState.distanceToPlayer < prevState.distanceToPlayer) {
            reward += 1.0; // Getting closer to player
        } else {
            reward -= 0.5; // Moving away from player
        }
        
        // Action-specific rewards
        switch (action) {
            case BREAK_BLOCK_DIRECT:
                if (currentState.hasLineOfSight && !prevState.hasLineOfSight) {
                    reward += 5.0; // Successfully cleared path
                } else if (currentState.blocksInPath < prevState.blocksInPath) {
                    reward += 2.0; // Removed obstacles
                }
                break;
                
            case BREAK_BLOCK_PREDICTIVE:
                if (currentState.distanceToPlayer < prevState.distanceToPlayer) {
                    reward += 3.0; // Smart pathfinding
                }
                break;
                
            case ATTACK_MELEE:
                if (currentState.distanceToPlayer < 3.0) {
                    reward += 10.0; // Successful attack range
                }
                break;
                
            case FLANK_LEFT:
            case FLANK_RIGHT:
                if (currentState.hasLineOfSight && currentState.distanceToPlayer < 8.0) {
                    reward += 4.0; // Successful flanking
                }
                break;
                
            case WAIT_AND_OBSERVE:
                if (currentState.playerIsMoving) {
                    reward += 1.0; // Good timing to observe
                }
                break;
        }
        
        // Penalty for taking too long
        reward -= 0.1;
        
        // Bonus for adaptive behavior based on player equipment
        if (currentState.playerArmor > 2 && (action == Action.BREAK_BLOCK_PREDICTIVE || action == Action.FLANK_LEFT || action == Action.FLANK_RIGHT)) {
            reward += 2.0; // Adapting to well-equipped player
        }
        
        return reward;
    }
    
    // Training method - call this periodically
    public void trainFromExperience() {
        if (experienceBuffer.size() < 10) return;
        
        // Sample random experiences for training
        Collections.shuffle(experienceBuffer);
        int sampleSize = Math.min(32, experienceBuffer.size());
        
        for (int i = 0; i < sampleSize; i++) {
            Experience exp = experienceBuffer.get(i);
            double currentQ = getQValue(exp.state, exp.action);
            double maxNextQ = exp.isTerminal ? 0 : getMaxQValue(exp.nextState);
            double newQ = currentQ + LEARNING_RATE * (exp.reward + DISCOUNT_FACTOR * maxNextQ - currentQ);
            setQValue(exp.state, exp.action, newQ);
        }
        
        setDirty();
    }
    
    // Encounter tracking
    public void recordEncounter(boolean entityWon) {
        totalEncounters++;
        if (entityWon) {
            successfulKills++;
        } else {
            playerEscapes++;
        }
        setDirty();
    }
    
    // Get performance metrics
    public double getSuccessRate() {
        return totalEncounters > 0 ? (double) successfulKills / totalEncounters : 0.0;
    }
    
    public double getExplorationRate() {
        return currentExplorationRate;
    }
    
    public int getTotalEncounters() {
        return totalEncounters;
    }
    
    public int getSuccessfulKills() {
        return successfulKills;
    }
    
    public int getPlayerEscapes() {
        return playerEscapes;
    }
    
    // Manual exploration rate adjustment
    public void setExplorationRate(double rate) {
        currentExplorationRate = Math.max(0.0, Math.min(1.0, rate));
        setDirty();
    }
    
    // Get Q-table size (for debugging)
    public int getQTableSize() {
        return qTable.size();
    }
    
    // Get total state-action pairs learned
    public int getTotalStateActionPairs() {
        return qTable.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
    
    // Reset learning (for testing)
    public void resetLearning() {
        qTable.clear();
        experienceBuffer.clear();
        currentExplorationRate = EXPLORATION_RATE;
        totalEncounters = 0;
        successfulKills = 0;
        playerEscapes = 0;
        setDirty();
    }
    
    // Advanced reward shaping based on player achievements
    public void adjustRewardsForPlayerProgress(Player player) {
        // You can integrate with Minecraft's advancement system here
        // For example, if player has "Suit Up" advancement, AI gets bonus rewards for tactical play
        
        // This is a placeholder - you'd implement actual advancement checking
        boolean hasAdvancedArmor = player.getArmorValue() > 15;
        boolean hasAdvancedWeapons = player.getMainHandItem().getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS) > 0;
        
        if (hasAdvancedArmor || hasAdvancedWeapons) {
            // Increase learning rate temporarily for more challenge
            // This makes the AI adapt faster to well-equipped players
            System.out.println("Player has advanced gear - AI entering adaptive learning mode");
        }
    }
    
    // Evaluate current strategy effectiveness
    public String getCurrentStrategyAssessment() {
        if (totalEncounters < 3) {
            return "Gathering initial data";
        }
        
        double recentSuccessRate = getSuccessRate();
        
        if (recentSuccessRate > 0.7) {
            return "Dominant strategy - highly effective";
        } else if (recentSuccessRate > 0.5) {
            return "Balanced strategy - moderately effective";
        } else if (recentSuccessRate > 0.3) {
            return "Developing strategy - improving";
        } else {
            return "Struggling strategy - needs adaptation";
        }
    }
    
    // Get most successful action for debugging
    public String getMostSuccessfulAction() {
        String bestAction = "UNKNOWN";
        double bestValue = Double.NEGATIVE_INFINITY;
        
        for (Map<String, Double> stateActions : qTable.values()) {
            for (Map.Entry<String, Double> actionValue : stateActions.entrySet()) {
                if (actionValue.getValue() > bestValue) {
                    bestValue = actionValue.getValue();
                    bestAction = actionValue.getKey();
                }
            }
        }
        
        return bestAction;
    }
    
    // Dynamic difficulty adjustment
    public void adjustDifficultyBasedOnPerformance(C2Entity entity) {
        double successRate = getSuccessRate();
        
        if (successRate > 0.8 && totalEncounters > 5) {
            // AI is too successful, make it slightly less aggressive
            currentExplorationRate = Math.min(0.3, currentExplorationRate + 0.05);
        } else if (successRate < 0.2 && totalEncounters > 5) {
            // AI is struggling, make it more exploratory
            currentExplorationRate = Math.max(0.05, currentExplorationRate - 0.05);
        }
        
        setDirty();
    }
    
    // Helper methods
    private int countBlocksInPath(C2Entity entity, Player player) {
        // Simple implementation - count non-air blocks in straight line
        BlockPos entityPos = entity.blockPosition();
        BlockPos playerPos = player.blockPosition();
        
        int dx = Math.abs(playerPos.getX() - entityPos.getX());
        int dy = Math.abs(playerPos.getY() - entityPos.getY());
        int dz = Math.abs(playerPos.getZ() - entityPos.getZ());
        
        return Math.max(dx, Math.max(dy, dz)); // Simplified
    }
    
    private int calculatePlayerArmorLevel(Player player) {
        // Simple armor calculation (0-4 scale)
        return Math.min(4, (int) (player.getArmorValue() / 5.0));
    }
    
    // NBT serialization
    private CompoundTag saveToNBT(CompoundTag tag) {
        // Save Q-table
        CompoundTag qTableTag = new CompoundTag();
        for (Map.Entry<String, Map<String, Double>> stateEntry : qTable.entrySet()) {
            CompoundTag stateTag = new CompoundTag();
            for (Map.Entry<String, Double> actionEntry : stateEntry.getValue().entrySet()) {
                stateTag.putDouble(actionEntry.getKey(), actionEntry.getValue());
            }
            qTableTag.put(stateEntry.getKey(), stateTag);
        }
        tag.put("qTable", qTableTag);
        
        // Save parameters
        tag.putDouble("explorationRate", currentExplorationRate);
        tag.putInt("totalEncounters", totalEncounters);
        tag.putInt("successfulKills", successfulKills);
        tag.putInt("playerEscapes", playerEscapes);
        
        return tag;
    }
    
    private void loadFromNBT(CompoundTag tag) {
        // Load Q-table
        if (tag.contains("qTable")) {
            CompoundTag qTableTag = tag.getCompound("qTable");
            for (String stateKey : qTableTag.getAllKeys()) {
                CompoundTag stateTag = qTableTag.getCompound(stateKey);
                Map<String, Double> stateActions = new HashMap<>();
                for (String actionKey : stateTag.getAllKeys()) {
                    stateActions.put(actionKey, stateTag.getDouble(actionKey));
                }
                qTable.put(stateKey, stateActions);
            }
        }
        
        // Load parameters
        currentExplorationRate = tag.getDouble("explorationRate");
        totalEncounters = tag.getInt("totalEncounters");
        successfulKills = tag.getInt("successfulKills");
        playerEscapes = tag.getInt("playerEscapes");
        
        if (currentExplorationRate == 0) {
            currentExplorationRate = EXPLORATION_RATE;
        }
    }
}