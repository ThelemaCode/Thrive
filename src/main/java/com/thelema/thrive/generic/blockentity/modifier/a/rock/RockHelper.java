package com.thelema.thrive.generic.blockentity.modifier.a.rock;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.PosApplier;
import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.constant.BiomeConstant;
import com.thelema.thrive.register.constant.TagConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;


final class RockHelper {

    private RockHelper() {}

    static void terrain(BiomeCoreBlockEntity be, BlockPos p) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        if (p.getY() >= 200) return;
        BlockState s = level.getBlockState(p);
        if (s.isAir()) return;
        if (s.is(BlockRegister.BIOME_CORE.get())) return;

        if (s.is(TagConstant.NATURAL_SURFACE)) {
            // 地表层 → 石头
            level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        } else if (s.is(TagConstant.NATURAL_COVER)) {
            // 覆盖层 → 空气
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** 跳过已是 rock 的坐标。用 getNoiseBiome 精确读，绕过 blur。 */
    static PosApplier skipRock(PosApplier inner) {
        return (be, p) -> {
            if (!(be.getLevel() instanceof ServerLevel sl)) return;
            Holder<Biome> b = sl.getChunk(p).getNoiseBiome(
                    p.getX() >> 2, p.getY() >> 2, p.getZ() >> 2);
            if (!b.is(BiomeConstant.ROCK)) inner.apply(be, p);
        };
    }
}