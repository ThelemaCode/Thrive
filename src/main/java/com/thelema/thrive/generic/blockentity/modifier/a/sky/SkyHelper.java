package com.thelema.thrive.generic.blockentity.modifier.a.sky;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;


final class SkyHelper {

    private SkyHelper() {}


    static boolean isSkyPlant(BlockState s) {
        return s.is(BlockRegister.CLOUD.get())
                || s.is(BlockRegister.WISPY_CLOUD.get());
    }

    static void clear(BiomeCoreBlockEntity be, BlockPos p) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        BlockState s = level.getBlockState(p);
        if (s.isAir()) return;
        if (s.is(BlockRegister.BIOME_CORE.get())) return;
        if (!isSkyPlant(s)) return;
        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
}