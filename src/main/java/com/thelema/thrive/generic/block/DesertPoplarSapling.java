package com.thelema.thrive.generic.block;

import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.constant.TreeGrowerConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;

/**
 * 胡杨树苗：走原版 {@link TreeGrower}，长的是
 * {@code data/thrive/worldgen/configured_feature/desert_poplar.json} 里的 {@code minecraft:tree} 地物。
 *
 * <p>和 {@code FancySapling}（NBT 结构）不是一套体系：地物是随机生成的，形状自然但不可精确复刻；
 * NBT 结构则相反。胡杨抄的是参考模组的地物，所以用这条线。
 *
 * <p>唯一和原版不同的地方是 {@link #mayPlaceOn}：胡杨长在沙地/尘土上。
 */
public class DesertPoplarSapling extends SaplingBlock {


    public DesertPoplarSapling() {
        super(TreeGrowerConstant.DESERT_POPLAR, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return canPlaceOn(state);
    }

    public static boolean canPlaceOn(BlockState state) {
        return state.is(BlockTags.SAND)
                || state.is(BlockRegister.DUST_BLOCK)
                || state.is(BlockTags.DIRT)
                || state.getBlock() instanceof FarmBlock;
    }
}
