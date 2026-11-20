package com.thelema.thrive.generic.block.wood;

import com.thelema.thrive.generic.blockentity.ThriveHangingSignBlockEntity;
import com.thelema.thrive.generic.blockentity.ThriveSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.NotNull;

/**
 * 告示牌四种形态。只在 {@code newBlockEntity} 上和原版不同：换成带自己
 * {@code BlockEntityType} 的方块实体。
 */
public final class WoodSignBlocks {

    private WoodSignBlocks() {
    }

    /** 立式告示牌（有物品形态）。 */
    public static class Standing extends StandingSignBlock {
        public Standing(WoodType type, Properties properties) {
            super(type, properties);
        }

        @Override
        public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
            return new ThriveSignBlockEntity(pos, state);
        }
    }

    /** 墙上的告示牌（无物品形态，放置时由立式转换）。 */
    public static class Wall extends WallSignBlock {
        public Wall(WoodType type, Properties properties) {
            super(type, properties);
        }

        @Override
        public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
            return new ThriveSignBlockEntity(pos, state);
        }
    }

    /** 悬挂式告示牌（有物品形态）。 */
    public static class CeilingHanging extends CeilingHangingSignBlock {
        public CeilingHanging(WoodType type, Properties properties) {
            super(type, properties);
        }

        @Override
        public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
            return new ThriveHangingSignBlockEntity(pos, state);
        }
    }

    /** 墙上的悬挂式告示牌（无物品形态）。 */
    public static class WallHanging extends WallHangingSignBlock {
        public WallHanging(WoodType type, Properties properties) {
            super(type, properties);
        }

        @Override
        public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
            return new ThriveHangingSignBlockEntity(pos, state);
        }
    }
}
