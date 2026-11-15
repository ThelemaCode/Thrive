package com.thelema.thrive.generic.blockentity.modifier.a.sea;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.markDirty;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.set;

final class SeaHelper {

    private SeaHelper() {}

    static boolean isOceanPlant(BlockState s) {
        return s.is(BlockTags.CORAL_BLOCKS)
                || s.is(BlockTags.CORAL_PLANTS)
                || s.is(BlockTags.WALL_CORALS)
                || s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT)
                || s.is(Blocks.SEAGRASS) || s.is(Blocks.TALL_SEAGRASS)
                || s.is(Blocks.SEA_PICKLE);
    }

    /** 清单个方块：海洋植物 → 海平面下变水，上变空气。 */
    static void clearBlock(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!isOceanPlant(s)) return;
        BlockState to = p.getY() < level.getSeaLevel()
                ? Blocks.WATER.defaultBlockState()
                : Blocks.AIR.defaultBlockState();
        set(level, p, to);
        markDirty(level, p);
    }

    static void clear(BiomeCoreBlockEntity be, BlockPos p) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        if (p.getY() >= 200) return;
        if (level.getBlockState(p).is(BlockRegister.BIOME_CORE.get())) return;
        clearBlock(level, p);
    }
}