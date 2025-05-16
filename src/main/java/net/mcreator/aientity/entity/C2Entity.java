package net.mcreator.aientity.entity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
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
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.aientity.init.AiEntityModEntities;

import java.util.List;

public class C2Entity extends Monster {
    private static int successfulFollows = 0;
    private static int failedFollows = 0;
    private int followTicks = 0;
    private boolean hasPlayerInRange = false;

    public C2Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(AiEntityModEntities.C_2.get(), world);
    }

    public C2Entity(EntityType<C2Entity> type, Level world) {
        super(type, world);
        setMaxUpStep(0.6f);
        xpReward = 0;
        setNoAi(false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new BlockBreakingGoal(this));
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.2));

        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            protected double getAttackReachSqr(LivingEntity entity) {
                return this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth();
            }
        });
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            List<ServerPlayer> players = serverLevel.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(16));
            if (!players.isEmpty()) {
                hasPlayerInRange = true;
                followTicks++;
            } else {
                hasPlayerInRange = false;
            }

            // Reward logic while alive
            if (followTicks > 100 && followTicks % 20 == 0) {
                successfulFollows++;
                followTicks = 0; // Reset to track more follow events
                System.out.println("Reward: Good follow behavior!");
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (hasPlayerInRange) {
            successfulFollows++;
        } else {
            failedFollows++;
        }
        System.out.println("Successful Follows: " + successfulFollows + ", Failed Follows: " + failedFollows);
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
        SpawnPlacements.register(AiEntityModEntities.C_2.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, world, reason, pos, random) -> world.getEntitiesOfClass(C2Entity.class, new AABB(pos).inflate(1000)).isEmpty()
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

    // === Custom goal to follow player ===
    public static class FollowPlayerGoal extends Goal {
        private final C2Entity mob;
        private final double speed;

        public FollowPlayerGoal(C2Entity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
        }

        @Override
        public boolean canUse() {
            // Get nearest player within 64 blocks (including through walls)
            var player = mob.level().getNearestPlayer(mob, 64);
            if (player instanceof ServerPlayer target) {
                mob.setTarget(target); // so block-breaking can work
                return true;
            }
            return false;
        }

        @Override
        public void tick() {
            // Move towards the nearest ServerPlayer within 64 blocks
            List<ServerPlayer> players = mob.level().getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(64));
            if (!players.isEmpty()) {
                ServerPlayer nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (ServerPlayer sp : players) {
                    double dist = mob.distanceToSqr(sp);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = sp;
                    }
                }
                if (nearest != null) {
                    mob.getNavigation().moveTo(nearest, speed);
                }
            }
        }
    }
}
