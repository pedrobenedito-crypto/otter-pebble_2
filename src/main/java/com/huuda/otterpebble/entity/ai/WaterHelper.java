package com.huuda.otterpebble.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Bank, landing and hazard checks shared by the water behaviours. */
public final class WaterHelper {

    /** A bank entry: walk/slide/hop toward {@code direction}; {@code water} is the landing block. */
    public record Entry(Direction direction, BlockPos water, int distance) {
    }

    public static boolean isWater(LevelReader level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    public static boolean isPassable(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isHazard(LevelReader level, BlockPos pos) {
        if (level.getFluidState(pos).is(FluidTags.LAVA)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.MAGMA_BLOCK) || state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES)
                || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POINTED_DRIPSTONE);
    }

    public static boolean isAreaSafe(LevelReader level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (isHazard(level, pos)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasWaterNearby(LevelReader level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 1, radius))) {
            if (isWater(level, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Water with open air above, at least {@code minDepth} blocks deep and no hazard around it. */
    public static boolean isSafeLanding(LevelReader level, BlockPos water, int minDepth) {
        if (!isWater(level, water) || !isPassable(level, water.above())) {
            return false;
        }
        for (int i = 1; i < minDepth; i++) {
            if (!isWater(level, water.below(i))) {
                return false;
            }
        }
        return isAreaSafe(level, water, 2);
    }

    /** Land the otter can stand on: solid floor, free space, no hazard. */
    public static boolean isStandable(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && isPassable(level, pos) && isPassable(level, pos.above())
                && !isWater(level, pos) && !isHazard(level, pos) && !isHazard(level, below);
    }

    /**
     * Looks for a clear bank: from the mob's position, a straight line of free blocks toward water that is
     * level with the mob or up to two blocks lower, with a safe landing.
     */
    @Nullable
    public static Entry findEntry(Mob mob, int maxDistance, int minDepth) {
        LevelReader level = mob.level();
        BlockPos origin = mob.blockPosition();
        List<Direction> directions = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        Collections.shuffle(directions, new java.util.Random(mob.getRandom().nextLong()));
        for (Direction direction : directions) {
            Entry entry = scanDirection(level, origin, direction, maxDistance, minDepth);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    private static Entry scanDirection(LevelReader level, BlockPos origin, Direction direction, int maxDistance, int minDepth) {
        BlockPos column = origin;
        for (int d = 1; d <= maxDistance; d++) {
            column = column.relative(direction);
            if (!isPassable(level, column.above())) {
                return null; // something blocks the body: not a clear bank
            }
            for (int dy = 0; dy >= -2; dy--) {
                BlockPos pos = column.below(-dy);
                if (isWater(level, pos)) {
                    return isSafeLanding(level, pos, minDepth) ? new Entry(direction, pos.immutable(), d) : null;
                }
                if (!isPassable(level, pos)) {
                    break; // ground at this height: keep walking along the bank
                }
                if (dy == -2) {
                    return null; // a drop without water below
                }
            }
            if (!isPassable(level, column)) {
                return null; // a wall at body height
            }
            if (isHazard(level, column) || isHazard(level, column.below())) {
                return null;
            }
        }
        return null;
    }

    /** Nearest land spot next to a safe landing (the pup's "own safe route" to the water). */
    public static Optional<BlockPos> findNearestBankSpot(Mob mob, int radius) {
        LevelReader level = mob.level();
        return BlockPos.findClosestMatch(mob.blockPosition(), radius, 3,
                        pos -> isStandable(level, pos) && waterDirectionFrom(level, pos) != null)
                .map(BlockPos::immutable);
    }

    @Nullable
    public static Direction waterDirectionFrom(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = pos.relative(direction);
            if ((isWater(level, next) && isSafeLanding(level, next, 1))
                    || (isWater(level, next.below()) && isSafeLanding(level, next.below(), 1))) {
                return direction;
            }
        }
        return null;
    }

    private WaterHelper() {
    }
}
