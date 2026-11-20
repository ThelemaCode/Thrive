package com.thelema.thrive.generic.block.wood;

import com.thelema.thrive.register.ParticleRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 胡杨树叶：普通树叶 + 飘落叶。
 *
 * <p>只在叶子**下方那一格不是实心**时才喷粒子（`isFaceFull` 判的是下方方块的碰撞形状顶面），
 * 所以埋在树冠内部的叶子不会拼命掉叶子。
 *
 * <p>燃烧属性走 {@code FireBlock#setFlammable}，不在这里覆写。
 */
public class DesertPoplarLeaves extends LeavesBlock {

    public DesertPoplarLeaves(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(20) != 0) return;

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!isFaceFull(belowState.getCollisionShape(level, below), Direction.UP)) {
            ParticleUtils.spawnParticleBelow(level, pos, random, ParticleRegister.DESERT_POPLAR_LEAVES.get());
        }
    }
}
