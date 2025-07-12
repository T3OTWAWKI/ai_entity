package net.mcreator.aientity.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class AIConfig {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Learning Parameters
    public static final ForgeConfigSpec.DoubleValue LEARNING_RATE;
    public static final ForgeConfigSpec.DoubleValue DISCOUNT_FACTOR;
    public static final ForgeConfigSpec.DoubleValue EXPLORATION_RATE;
    public static final ForgeConfigSpec.DoubleValue EXPLORATION_DECAY;
    public static final ForgeConfigSpec.IntValue EXPERIENCE_BUFFER_SIZE;
    
    // Behavior Parameters
    public static final ForgeConfigSpec.IntValue DECISION_COOLDOWN;
    public static final ForgeConfigSpec.IntValue TRAINING_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue BASE_MOVEMENT_SPEED;
    public static final ForgeConfigSpec.IntValue BLOCK_BREAK_TIME;
    
    // Reward Values
    public static final ForgeConfigSpec.DoubleValue REWARD_GETTING_CLOSER;
    public static final ForgeConfigSpec.DoubleValue PENALTY_MOVING_AWAY;
    public static final ForgeConfigSpec.DoubleValue REWARD_SUCCESSFUL_ATTACK;
    public static final ForgeConfigSpec.DoubleValue REWARD_CLEAR_PATH;
    public static final ForgeConfigSpec.DoubleValue REWARD_SMART_PATHFINDING;
    public static final ForgeConfigSpec.DoubleValue REWARD_FLANKING;
    public static final ForgeConfigSpec.DoubleValue PENALTY_TIME_TAKEN;
    
    // Adaptive Features
    public static final ForgeConfigSpec.BooleanValue ENABLE_DIFFICULTY_ADJUSTMENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PLAYER_PROGRESS_SCALING;
    public static final ForgeConfigSpec.DoubleValue DIFFICULTY_ADJUSTMENT_THRESHOLD;
    
    // Debug Options
    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ACTION_LOGGING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_REWARD_LOGGING;
    
    static {
        BUILDER.comment("AI Entity Reinforcement Learning Configuration")
                .push("learning");
        
        LEARNING_RATE = BUILDER
                .comment("How quickly the AI learns from new experiences (0.0 - 1.0)")
                .defineInRange("learningRate", 0.1, 0.0, 1.0);
        
        DISCOUNT_FACTOR = BUILDER
                .comment("How much the AI values future rewards vs immediate rewards (0.0 - 1.0)")
                .defineInRange("discountFactor", 0.95, 0.0, 1.0);
        
        EXPLORATION_RATE = BUILDER
                .comment("Initial chance of taking random actions vs learned actions (0.0 - 1.0)")
                .defineInRange("explorationRate", 0.1, 0.0, 1.0);
        
        EXPLORATION_DECAY = BUILDER
                .comment("How quickly exploration decreases over time (0.0 - 1.0)")
                .defineInRange("explorationDecay", 0.995, 0.0, 1.0);
        
        EXPERIENCE_BUFFER_SIZE = BUILDER
                .comment("Maximum number of experiences to keep for training")
                .defineInRange("experienceBufferSize", 1000, 100, 10000);
        
        BUILDER.pop().push("behavior");
        
        DECISION_COOLDOWN = BUILDER
                .comment("Ticks between AI decisions (20 ticks = 1 second)")
                .defineInRange("decisionCooldown", 20, 1, 100);
        
        TRAINING_INTERVAL = BUILDER
                .comment("Ticks between training sessions (200 ticks = 10 seconds)")
                .defineInRange("trainingInterval", 200, 20, 1000);
        
        BASE_MOVEMENT_SPEED = BUILDER
                .comment("Base movement speed multiplier for AI entity")
                .defineInRange("baseMovementSpeed", 1.2, 0.1, 3.0);
        
        BLOCK_BREAK_TIME = BUILDER
                .comment("Ticks required to break a block")
                .defineInRange("blockBreakTime", 8, 1, 100);
        
        BUILDER.pop().push("rewards");
        
        REWARD_GETTING_CLOSER = BUILDER
                .comment("Reward for getting closer to player")
                .defineInRange("rewardGettingCloser", 1.0, 0.0, 10.0);
        
        PENALTY_MOVING_AWAY = BUILDER
                .comment("Penalty for moving away from player")
                .defineInRange("penaltyMovingAway", -0.5, -10.0, 0.0);
        
        REWARD_SUCCESSFUL_ATTACK = BUILDER
                .comment("Reward for successfully attacking player")
                .defineInRange("rewardSuccessfulAttack", 50.0, 0.0, 100.0);
        
        REWARD_CLEAR_PATH = BUILDER
                .comment("Reward for clearing path to player")
                .defineInRange("rewardClearPath", 5.0, 0.0, 20.0);
        
        REWARD_SMART_PATHFINDING = BUILDER
                .comment("Reward for predictive pathfinding")
                .defineInRange("rewardSmartPathfinding", 3.0, 0.0, 10.0);
        
        REWARD_FLANKING = BUILDER
                .comment("Reward for successful flanking maneuvers")
                .defineInRange("rewardFlanking", 4.0, 0.0, 10.0);
        
        PENALTY_TIME_TAKEN = BUILDER
                .comment("Penalty applied each tick to encourage efficiency")
                .defineInRange("penaltyTimeTaken", -0.1, -1.0, 0.0);
        
        BUILDER.pop().push("adaptive");
        
        ENABLE_DIFFICULTY_ADJUSTMENT = BUILDER
                .comment("Whether AI should adjust difficulty based on performance")
                .define("enableDifficultyAdjustment", true);
        
        ENABLE_PLAYER_PROGRESS_SCALING = BUILDER
                .comment("Whether AI should scale with player advancement progress")
                .define("enablePlayerProgressScaling", true);
        
        DIFFICULTY_ADJUSTMENT_THRESHOLD = BUILDER
                .comment("Success rate threshold for difficulty adjustment")
                .defineInRange("difficultyAdjustmentThreshold", 0.7, 0.0, 1.0);
        
        BUILDER.pop().push("debug");
        
        ENABLE_DEBUG_LOGGING = BUILDER
                .comment("Enable general debug logging")
                .define("enableDebugLogging", false);
        
        ENABLE_ACTION_LOGGING = BUILDER
                .comment("Enable logging of AI actions")
                .define("enableActionLogging", false);
        
        ENABLE_REWARD_LOGGING = BUILDER
                .comment("Enable logging of reward calculations")
                .define("enableRewardLogging", false);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
    
    // Utility methods for easy access
    public static double getLearningRate() {
        return LEARNING_RATE.get();
    }
    
    public static double getDiscountFactor() {
        return DISCOUNT_FACTOR.get();
    }
    
    public static double getExplorationRate() {
        return EXPLORATION_RATE.get();
    }
    
    public static double getExplorationDecay() {
        return EXPLORATION_DECAY.get();
    }
    
    public static int getExperienceBufferSize() {
        return EXPERIENCE_BUFFER_SIZE.get();
    }
    
    public static int getDecisionCooldown() {
        return DECISION_COOLDOWN.get();
    }
    
    public static int getTrainingInterval() {
        return TRAINING_INTERVAL.get();
    }
    
    public static double getBaseMovementSpeed() {
        return BASE_MOVEMENT_SPEED.get();
    }
    
    public static int getBlockBreakTime() {
        return BLOCK_BREAK_TIME.get();
    }
    
    public static boolean isDebugLoggingEnabled() {
        return ENABLE_DEBUG_LOGGING.get();
    }
    
    public static boolean isActionLoggingEnabled() {
        return ENABLE_ACTION_LOGGING.get();
    }
    
    public static boolean isRewardLoggingEnabled() {
        return ENABLE_REWARD_LOGGING.get();
    }
    
    public static boolean isDifficultyAdjustmentEnabled() {
        return ENABLE_DIFFICULTY_ADJUSTMENT.get();
    }
    
    public static boolean isPlayerProgressScalingEnabled() {
        return ENABLE_PLAYER_PROGRESS_SCALING.get();
    }
}