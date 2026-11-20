package com.thelema.thrive.generic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class DustBlock extends ColoredFallingBlock {

    public DustBlock() {
        super(new ColorRGBA(14406560), BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).randomTicks());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel sl, BlockPos pos, RandomSource random) {
        if (sl.getBlockState(pos.north()).is(Blocks.WATER)
                && sl.getBlockState(pos.east()).is(Blocks.WATER)
                && sl.getBlockState(pos.south()).is(Blocks.WATER)
                && sl.getBlockState(pos.west()).is(Blocks.WATER)) {
            sl.setBlock(pos, Blocks.CLAY.defaultBlockState(), 2);
        }
    }
}
