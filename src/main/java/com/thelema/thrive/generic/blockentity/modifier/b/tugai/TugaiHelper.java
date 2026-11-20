package com.thelema.thrive.generic.blockentity.modifier.b.tugai;

import com.thelema.thrive.generic.block.DesertPoplarSapling;
import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.feature.Lake;
import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.constant.ConfiguredFeatureConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;


/**
 * 荒漠 → 胡杨林（Tugai）的地表、水池、植被。
 *
 * <p>几个硬约束：
 * <ul>
 *   <li>树必须用 {@code ConfiguredFeature.place} 直接放，**不能**先放树苗再 {@code TreeGrower.growTree}：
 *       growTree 会先把树苗那格清成空气、再要求 feature 放得下，失败时静默还原，看起来就是"树苗不长"。</li>
 *   <li>水池必须在树之前挖，否则树会长在水里。</li>
 *   <li>草必须在树之后种，否则 {@code p.above()} 不是空气，树全被跳过。</li>
 * </ul>
 */
public final class TugaiHelper {

    private TugaiHelper() {}

    /** 每个地表采样点变成水池的概率倒数。 */
    static final int POOL_CHANCE = 2;
    /** 每个地表采样点长草的概率倒数。 */
    static final int GRASS_CHANCE = 2;
    /** 每个地表采样点长树的概率倒数。 */
    static final int TREE_CHANCE = 2;
    /** 树里有多少比例是枯树。 */
    static final int DEAD_TREE_CHANCE = 5;

    // =======================  地表材质  =====================================

    /** 沙 → 尘土；砂岩 → 方解石/盐块交替。 */
    static void surface(BiomeCoreBlockEntity be, BlockPos p) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;

        BlockState s = level.getBlockState(p);
        if (s.isAir()) return;

        if (s.is(Blocks.SAND)) {
            level.setBlock(p, BlockRegister.DUST_BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        } else if (s.is(Blocks.SANDSTONE)) {
            if (p.getX() % 3 != 0) {
                level.setBlock(p, Blocks.CALCITE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            } else {
                level.setBlock(p, BlockRegister.SALT_BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    // =======================  水池  =========================================

    /**
     * 浅水池。形状交给 {@link Lake}——它自带边界校验（水位、坡度、液体相邻）和屏障层，
     * 比自己写的"挖个圆坑"结实得多。
     *
     * <p>碱块当 barrier 参数传进去，湖沿自然被它包一圈。
     */
    static void pool(BiomeCoreBlockEntity be, BlockPos p) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;

        BlockState ground = level.getBlockState(p);
        if (!ground.is(BlockRegister.DUST_BLOCK.get())) return;
        if (level.random.nextInt(POOL_CHANCE) != 0) return;

        Lake.placeAt(level, p, level.random, Blocks.WATER.defaultBlockState(), BlockRegister.ALKALI_BLOCK.get().defaultBlockState());
    }

    private static boolean isSoil(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.is(BlockRegister.DUST_BLOCK.get()) || s.is(Blocks.SAND)
                || s.is(Blocks.CALCITE) || s.is(BlockRegister.SALT_BLOCK.get());
    }

    // =======================  树  ===========================================

    /** 胡杨树，少量枯树。水面附近不长。 */
    static void tree(BiomeCoreBlockEntity be, BlockPos p) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        if (!DesertPoplarSapling.canPlaceOn(level.getBlockState(p))) return;

        BlockPos base = p.above();
        if (!level.getBlockState(base).isAir()) return;
        if (nearWater(level, p, 2)) return;
        if (level.random.nextInt(TREE_CHANCE) != 0) return;

        if (level.random.nextInt(DEAD_TREE_CHANCE) == 0) {
            deadTree(level, base);
        } else {
            growPoplar(level, base);
        }
    }

    /**
     * 直接放 configured feature，不走树苗。
     *
     * <p>{@code TreeGrower.growTree} 会先清掉树苗那格再 place，失败就静默还原；
     * 而且它还带 2×2 大树的判定。这里只要 feature 本身，失败就失败，下一棵树补上。
     */
    static boolean growPoplar(ServerLevel level, BlockPos base) {
        return level.registryAccess()
                .lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ConfiguredFeatureConstant.DESERT_POPLAR_FEATURE)
                .value()
                .place(level, level.getChunkSource().getGenerator(), level.random, base);
    }

    /** 枯死的胡杨：一根 3~6 格的光杆，加一两根横枝。 */
    static void deadTree(ServerLevel level, BlockPos base) {
        RandomSource random = level.random;
        BlockState log = BlockRegister.DESERT_POPLAR_LOG.get().defaultBlockState();
        int height = 3 + random.nextInt(4);

        for (int i = 0; i < height; i++) {
            level.setBlock(base.above(i), log, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }

        int branches = 1 + random.nextInt(2);
        for (int b = 0; b < branches; b++) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos at = base.above(1 + random.nextInt(Math.max(1, height - 2)));
            int length = 1 + random.nextInt(2);
            for (int i = 0; i < length; i++) {
                at = at.relative(dir);
                level.setBlock(at, log.setValue(RotatedPillarBlock.AXIS, dir.getAxis()), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    /** 附近几格内有没有液体（水池边上不长树、不长草）。 */
    static boolean nearWater(ServerLevel level, BlockPos p, int r) {
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-r, 0, -r), p.offset(r, 1, r))) {
            if (!level.getFluidState(q).isEmpty()) return true;
        }
        return false;
    }

    // =======================  黄草  =========================================

    /** 茂密的黄草：以命中点为中心长出一个椭圆"草丘"，再对新草施 3 次骨粉。 */
    static void grass(BiomeCoreBlockEntity be, BlockPos p) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;

        BlockState ground = level.getBlockState(p);
        if (!ground.is(BlockRegister.DUST_BLOCK.get()) && !ground.is(Blocks.SAND)) return;
        if (!level.getBlockState(p.above()).isAir()) return;

        // 水边必长，别处按 GRASS_CHANCE 抽
        boolean edge = nearWater(level, p, 2);
        if (!edge && level.random.nextInt(GRASS_CHANCE) != 0) return;

        RandomSource random = level.random;
        int rx = 2 + random.nextInt(3);          // 椭圆半径 2~4
        int rz = 2 + random.nextInt(3);
        int height = 1 + random.nextInt(2);      // 柱高 1~2，斜坡上也能吃住

        BlockState grassBlock = BlockRegister.GRASS_BLOCK.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos seed = null;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                if ((double) (dx * dx) / (rx * rx) + (double) (dz * dz) / (rz * rz) > 1.0) continue;

                for (int dy = 0; dy < height; dy++) {
                    pos.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
                    if (!isSoil(level, pos)) continue;
                    if (!level.getBlockState(pos.above()).isAir()) continue;   // 只换暴露在空气里的
                    level.setBlock(pos, grassBlock, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    if (seed == null) seed = pos.immutable();
                }
            }
        }

        // 中心那格变绿了就以它为准，骨粉覆盖更均匀
        BlockPos target = level.getBlockState(p).is(BlockRegister.GRASS_BLOCK.get()) ? p : seed;
        if (target != null) bonemeal(level, target, 3);
    }

    /** 对草方块施骨粉，让它自己长草（草方块本身就是 BonemealableBlock）。 */
    private static void bonemeal(ServerLevel level, BlockPos pos, int times) {
        for (int i = 0; i < times; i++) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock growable)) return;
            if (!growable.isValidBonemealTarget(level, pos, state)) return;
            growable.performBonemeal(level, level.random, pos, state);
        }
    }
}