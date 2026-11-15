package com.thelema.thrive.generic.blockentity.modifier.a.rock;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.PosApplier;
import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.markDirty;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.set;

final class RockHelper {

    private RockHelper() {}

    static final ResourceKey<Biome> TARGET = BiomeConstant.ROCK;


    static void terrain(BiomeCoreBlockEntity be, BlockPos p) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        if (p.getY() >= 200) return;
        BlockState s = level.getBlockState(p);
        if (s.isAir()) return;
        if (s.is(BlockRegister.BIOME_CORE.get())) return;

        // 1. 先排除所有“非自然”的功能性方块（箱子、熔炉、潜影盒等）
        //    这些方块哪怕材质是木头的，也不该被转换。
        if (s.is(BlockTags.GUARDED_BY_PIGLINS) || s.is(BlockTags.SHULKER_BOXES) || s.is(BlockTags.ANVIL) || s.is(BlockTags.BEACON_BASE_BLOCKS)) {
            return;
        }

        // 2. 核心判定：只要属于以下任意“自然方块”类别，就转换为石头
        boolean isNatural = s.is(BlockTags.DIRT)           // 泥土、草方块、菌丝、苔藓块等
                || s.is(BlockTags.SAND)                    // 沙子、红沙
                || s.is(BlockTags.LOGS)                    // 所有原木（含菌柄）
                || s.is(BlockTags.LEAVES)                  // 所有树叶
                || s.is(BlockTags.SAPLINGS)                // 树苗
                || s.is(BlockTags.FLOWERS)                 // 花
                || s.is(BlockTags.CROPS)                   // 农作物
                || s.is(BlockTags.REPLACEABLE)             // 可被替换的植物（草、蕨等）
                || s.is(BlockTags.SNOW)                    // 雪
                || s.is(BlockTags.ICE)                     // 冰
                || s.is(Blocks.CLAY) || s.is(Blocks.MUD)   // 黏土、泥巴
                || s.is(Blocks.DRIPSTONE_BLOCK) || s.is(Blocks.MAGMA_BLOCK) // 滴水石、岩浆块
                || s.is(Blocks.BONE_BLOCK) || s.is(Blocks.SCULK);   // 骨块、幽匿

        if (isNatural) {
            set(level, p, Blocks.STONE.defaultBlockState());
            markDirty(level, p);
        }
    }

    /** 跳过已是 rock 的坐标。用 getNoiseBiome 精确读，绕过 blur。 */
    static PosApplier skipRock(PosApplier inner) {
        return (be, p) -> {
            if (!(be.getLevel() instanceof ServerLevel sl)) return;
            Holder<Biome> b = sl.getChunk(p).getNoiseBiome(
                    p.getX() >> 2, p.getY() >> 2, p.getZ() >> 2);
            if (!b.is(TARGET)) inner.apply(be, p);
        };
    }
}