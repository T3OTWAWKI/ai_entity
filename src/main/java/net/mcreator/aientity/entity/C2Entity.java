package net.mcreator.aientity.entity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import net.mcreator.aientity.init.AiEntityModEntities;
import net.mcreator.aientity.learning.ReinforcementLearningSystem;
import net.mcreator.aientity.learning.ReinforcementLearningSystem.Action;
import net.mcreator.aientity.learning.ReinforcementLearningSystem.GameState;

import java.util.List;

public class C2Entity extends Monster {
    private static int successfulFollows = 0;
    private static int failedFollows = 0;
    private int followTicks = 0;
    private boolean hasPlayerInRange = false;
    
    // RL Integration
    private ReinforcementLearningSystem rlSystem;
    private GameState lastState;
    private Action lastAction;
    private int actionCooldown = 0;
    private int encounterStartTime = 0;
    private boolean inEncounter = false;
    
    public C2Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(AiEntityModEntities.C_2.get(), world);
    }

    public C2Entity(EntityType<C2Entity> type, Level world) {
        super(type, world);
        setMaxUpStep(0.6f);
        xpReward = 0;
        setNoAi(false);
        
        // Initialize RL system
        if (!world.isClientSide && world instanceof ServerLevel serverLevel) {
            rlSystem = ReinforcementLearningSystem.get(serverLevel);
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new SmartBlockBreakingGoal(this));
        this.goalSelector.addGoal(1, new AdaptiveFollowGoal(this, 1.2));
        this.goalSelector.addGoal(2, new SmartMeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64));
            
            if (!players.isEmpty()) {
                ServerPlayer nearestPlayer = getNearestPlayer(players);
                
                if (!inEncounter) {
                    // Start encounter
                    inEncounter = true;
                    encounterStartTime = this.tickCount;
                    rlSystem.recordEncounter(false); // Will be updated later
                }
                
                hasPlayerInRange = true;
                followTicks++;
                
                // RL Decision Making
                if (actionCooldown <= 0) {
                    makeRLDecision(nearestPlayer);
                    actionCooldown = 20; // Make decisions every second
                }
                
                actionCooldown--;
            } else {
                if (inEncounter) {
                    // End encounter - player escaped
                    inEncounter = false;
                    if (rlSystem != null) {
                        rlSystem.recordEncounter(false);
                    }
                }
                hasPlayerInRange = false;
            }

            // Train the AI periodically
            if (this.tickCount % 200 == 0) { // Every 10 seconds
                if (rlSystem != null) {
                    rlSystem.trainFromExperience();
                }
            }
        }
    }
    
    private void makeRLDecision(ServerPlayer player) {
        if (rlSystem == null) return;
        
        GameState currentState = rlSystem.getCurrentState(this, player);
        Action chosenAction = rlSystem.chooseAction(currentState);
        
        // Calculate reward for previous action
        if (lastState != null && lastAction != null) {
            double reward = rlSystem.calculateReward(this, player, lastAction, lastState, currentState);
            rlSystem.updateQTable(lastState.toStateString(), lastAction.name(), reward, currentState.toStateString(), false);
        }
        
        // Execute chosen action
        executeAction(chosenAction, player);
        
        // Update state tracking
        lastState = currentState;
        lastAction = chosenAction;
    }
    
    private void executeAction(Action action, ServerPlayer player) {
        switch (action) {
            case MOVE_TOWARDS_PLAYER:
                this.getNavigation().moveTo(player, 1.2);
                break;
                
            case BREAK_BLOCK_DIRECT:
                breakBlocksToPlayer(player, false);
                break;
                
            case BREAK_BLOCK_PREDICTIVE:
                breakBlocksToPlayer(player, true);
                break;
                
            case ATTACK_MELEE:
                if (this.distanceTo(player) < 3.0) {
                    this.doHurtTarget(player);
                }
                break;
                
            case FLANK_LEFT:
                flankPlayer(player, true);
                break;
                
            case FLANK_RIGHT:
                flankPlayer(player, false);
                break;
                
            case WAIT_AND_OBSERVE:
                // Stop movement and observe
                this.getNavigation().stop();
                this.getLookControl().setLookAt(player);
                break;
                
            case RETREAT_AND_REPOSITION:
                retreatAndReposition(player);
                break;
        }
    }
    
    private void breakBlocksToPlayer(ServerPlayer player, boolean predictive) {
        Vec3 entityPos = this.position();
        Vec3 playerPos = predictive ? predictPlayerPosition(player) : player.position();
        Vec3 direction = playerPos.subtract(entityPos).normalize();
        
        for (int i = 1; i <= 5; i++) {
            BlockPos targetPos = BlockPos.containing(entityPos.add(direction.scale(i)));
            if (!this.level().getBlockState(targetPos).isAir()) {
                this.level().destroyBlock(targetPos, true, this);
                break;
            }
        }
    }
    
    private Vec3 predictPlayerPosition(ServerPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        return player.position().add(velocity.scale(10)); // Predict 10 ticks ahead
    }
    
    private void flankPlayer(ServerPlayer player, boolean left) {
        Vec3 toPlayer = player.position().subtract(this.position()).normalize();
        Vec3 flankDirection = left ? 
            new Vec3(-toPlayer.z, 0, toPlayer.x) : 
            new Vec3(toPlayer.z, 0, -toPlayer.x);
        
        Vec3 targetPos = this.position().add(flankDirection.scale(8));
        this.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.5);
    }
    
    private void retreatAndReposition(ServerPlayer player) {
        Vec3 awayFromPlayer = this.position().subtract(player.position()).normalize();
        Vec3 retreatPos = this.position().add(awayFromPlayer.scale(10));
        this.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.8);
    }
    
    private ServerPlayer getNearestPlayer(List<ServerPlayer> players) {
        ServerPlayer nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (ServerPlayer player : players) {
            double dist = this.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }
        
        return nearest;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        
        // Record final outcome
        if (inEncounter && rlSystem != null) {
            rlSystem.recordEncounter(false); // Entity died, so it lost
        }
        
        if (hasPlayerInRange) {
            successfulFollows++;
        } else {
            failedFollows++;
        }
        
        System.out.println("AI Entity died. Total encounters: " + (rlSystem != null ? rlSystem.getTotalEncounters() : 0));
        System.out.println("Success rate: " + (rlSystem != null ? rlSystem.getSuccessRate() * 100 : 0) + "%");
    }
    
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean result = super.doHurtTarget(target);
        
        if (result && target instanceof ServerPlayer && inEncounter && rlSystem != null) {
            // Successful attack
            rlSystem.recordEncounter(true);
            
            // Give final reward for successful encounter
            if (lastState != null && lastAction != null) {
                rlSystem.updateQTable(lastState.toStateString(), lastAction.name(), 50.0, "", true);
            }
        }
        
        return result;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
    }

    @Override
    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
    }

    public static void init() {
        SpawnPlacements.register(AiEntityModEntities.C_2.get(), SpawnPlacements.Type.ON_GROUND, 
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, world, reason, pos, random) -> 
                    world.getEntitiesOfClass(C2Entity.class, new AABB(pos).inflate(1000)).isEmpty()
                    && world.getDifficulty() != Difficulty.PEACEFUL
                    && Monster.isDarkEnoughToSpawn(world, pos, random)
                    && Mob.checkMobSpawnRules(entityType, world, reason, pos, random));
    }

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        builder = builder.add(Attributes.MOVEMENT_SPEED, 0.4);
        builder = builder.add(Attributes.MAX_HEALTH, 20);
        builder = builder.add(Attributes.ARMOR, 2);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 4);
        builder = builder.add(Attributes.FOLLOW_RANGE, 64);
        return builder;
    }
    
    // Updated Goals with RL integration
    public static class SmartBlockBreakingGoal extends Goal {
        private final C2Entity mob;
        private BlockPos targetBlock;
        private int breakTime;

        public SmartBlockBreakingGoal(C2Entity mob) {
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            return mob.getTarget() instanceof ServerPlayer && mob.lastAction == Action.BREAK_BLOCK_DIRECT;
        }

        @Override
        public void start() {
            breakTime = 0;
            findTargetBlock();
        }

        private void findTargetBlock() {
            if (mob.getTarget() == null) return;
            
            Vec3 fromVec = Vec3.atCenterOf(mob.blockPosition());
            Vec3 toVec = Vec3.atCenterOf(mob.getTarget().blockPosition());
            Vec3 direction = toVec.subtract(fromVec).normalize();

            for (int i = 1; i <= 5; i++) {
                Vec3 checkVec = fromVec.add(direction.scale(i));
                BlockPos basePos = BlockPos.containing(checkVec);
                BlockPos[] positionsToCheck = { basePos, basePos.above() };

                for (BlockPos pos : positionsToCheck) {
                    if (!mob.level().getBlockState(pos).isAir()) {
                        targetBlock = pos;
                        return;
                    }
                }
            }
        }

        @Override
        public void tick() {
            if (targetBlock != null && mob.distanceToSqr(Vec3.atCenterOf(targetBlock)) <= 36) {
                breakTime++;
                mob.getLookControl().setLookAt(Vec3.atCenterOf(targetBlock));

                if (breakTime >= 8) { // Slightly faster breaking
                    mob.level().destroyBlock(targetBlock, true, mob);
                    breakTime = 0;
                    targetBlock = null;
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return targetBlock != null;
        }
    }
    
    public static class AdaptiveFollowGoal extends Goal {
        private final C2Entity mob;
        private final double speed;

        public AdaptiveFollowGoal(C2Entity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
        }

        @Override
        public boolean canUse() {
            var player = mob.level().getNearestPlayer(mob, 64);
            if (player instanceof ServerPlayer target) {
                mob.setTarget(target);
                return mob.lastAction == Action.MOVE_TOWARDS_PLAYER || mob.lastAction == Action.FLANK_LEFT || mob.lastAction == Action.FLANK_RIGHT;
            }
            return false;
        }

        @Override
        public void tick() {
            if (mob.getTarget() instanceof ServerPlayer player) {
                // Adaptive movement based on RL decision
                double adaptiveSpeed = speed;
                
                // Adjust speed based on player behavior
                if (player.isInWater()) {
                    adaptiveSpeed *= 1.3; // Faster when player is slowed
                }
                
                if (player.getHealth() < player.getMaxHealth() * 0.3) {
                    adaptiveSpeed *= 1.2; // More aggressive when player is low health
                }
                
                mob.getNavigation().moveTo(player, adaptiveSpeed);
            }
        }
    }
    
    public static class SmartMeleeAttackGoal extends MeleeAttackGoal {
        private final C2Entity smartMob;
        private int attackCooldown = 0;
        
        public SmartMeleeAttackGoal(C2Entity mob, double speed, boolean followTarget) {
            super(mob, speed, followTarget);
            this.smartMob = mob;
        }
        
        @Override
        public boolean canUse() {
            return super.canUse() && smartMob.lastAction == Action.ATTACK_MELEE;
        }
        
        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distanceToTarget) {
            double attackReach = this.getAttackReachSqr(target);
            
            if (distanceToTarget <= attackReach && attackCooldown <= 0) {
                this.mob.swing(this.mob.getUsedItemHand());
                this.mob.doHurtTarget(target);
                
                // Smart attack patterns based on learning
                if (smartMob.rlSystem != null) {
                    double successRate = smartMob.rlSystem.getSuccessRate();
                    if (successRate > 0.7) {
                        // If doing well, be more aggressive
                        attackCooldown = 15;
                    } else {
                        // If struggling, be more cautious
                        attackCooldown = 25;
                    }
                } else {
                    attackCooldown = 20; // Default cooldown
                }
            }
            
            if (attackCooldown > 0) {
                attackCooldown--;
            }
        }
        
        @Override
        protected double getAttackReachSqr(LivingEntity entity) {
            return this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth();
        }
    }
}