package net.mcreator.aientity.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BlockBreakingGoal extends Goal {
    private final C2Entity mob;
    private BlockPos targetBlock;
    private int breakTime;

    public BlockBreakingGoal(C2Entity mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        List<ServerPlayer> players = mob.level().getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(16));
        if (players.isEmpty()) return false;

        ServerPlayer player = players.get(0); // Just pick the first player in range

        Vec3 fromVec = Vec3.atCenterOf(mob.blockPosition());
        Vec3 toVec = Vec3.atCenterOf(player.blockPosition());
        Vec3 direction = toVec.subtract(fromVec).normalize();

        for (int i = 1; i <= 5; i++) {
            Vec3 checkVec = fromVec.add(direction.scale(i));
            BlockPos basePos = BlockPos.containing(checkVec);

            // Check two vertical levels: foot and head
            BlockPos[] positionsToCheck = { basePos, basePos.above() };

            for (BlockPos pos : positionsToCheck) {
                BlockState state = mob.level().getBlockState(pos);
                if (!state.isAir() && state.getDestroySpeed(mob.level(), pos) >= 0) {
                    targetBlock = pos;
                    System.out.println("Targeting block to break: " + pos + " | Block: " + state.getBlock());
                    return true;
                }
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
        if (targetBlock != null && mob.distanceToSqr(Vec3.atCenterOf(targetBlock)) <= 36) {
            breakTime++;

            // Face the block
            mob.getLookControl().setLookAt(Vec3.atCenterOf(targetBlock));

            if (breakTime >= 20) {
                System.out.println("Breaking block at: " + targetBlock);
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
