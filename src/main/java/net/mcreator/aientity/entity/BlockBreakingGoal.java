package net.mcreator.aientity.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlockBreakingGoal extends Goal {
    private final C2Entity mob;
    private BlockPos targetBlock;
    private int breakTime;

    public BlockBreakingGoal(C2Entity mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() == null)
            return false;

        Vec3 fromVec = mob.position().add(0, mob.getEyeHeight(), 0);
        Vec3 toVec = mob.getTarget().position().add(0, mob.getTarget().getEyeHeight(), 0);
        ClipContext context = new ClipContext(fromVec, toVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob);
        BlockHitResult result = mob.level().clip(context);

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = result.getBlockPos();
            BlockState state = mob.level().getBlockState(blockPos);

            // If it's glass, break it even if there's line of sight
            if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)) {
                targetBlock = blockPos;
                return true;
            }

            // Otherwise, only break solid blocks if there's no line of sight
            if (!mob.getSensing().hasLineOfSight(mob.getTarget()) &&
                !state.isAir() &&
                state.getDestroySpeed(mob.level(), blockPos) >= 0) {
                targetBlock = blockPos;
                return true;
            }
        }

        return false;
    }

    @Override
    public void start() {
        breakTime = 0;
    }

    @Override
    public void tick() {
        if (targetBlock != null && mob.distanceToSqr(Vec3.atCenterOf(targetBlock)) <= 16) {
            breakTime++;

            // Optionally face the block
            mob.getLookControl().setLookAt(Vec3.atCenterOf(targetBlock));

            if (breakTime >= 20) { // 2 seconds (20 ticks/second)
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
