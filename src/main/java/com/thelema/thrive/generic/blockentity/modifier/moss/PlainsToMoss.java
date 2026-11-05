package com.thelema.thrive.generic.blockentity.modifier.moss;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.PhasedModifier;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.markDirty;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.set;

/**
 * 平原 → Moss 一次性转换。
 * <p>
 * BE 在 plains 启动，REWRITE 把 Ring 内 plains 改成 moss，
 * 后续阶段靠 {@link MossHelper#onlyMoss} 只处理本次转换出的 moss，不碰原版/其他 moss。
 */
public class PlainsToMoss extends PhasedModifier {

    static final ResourceKey<Biome> SOURCE = ResourceKey.create(
            Registries.BIOME, ResourceLocation.withDefaultNamespace("plains"));

    static final Set<Block> REMOVE = Set.of(
            Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
            Blocks.DANDELION, Blocks.POPPY, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER,
            Blocks.AZURE_BLUET, Blocks.ORANGE_TULIP, Blocks.PINK_TULIP,
            Blocks.RED_TULIP, Blocks.WHITE_TULIP,
            Blocks.OAK_SAPLING, Blocks.OAK_LOG, Blocks.OAK_LEAVES,
            Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING);

    static final Set<Block> REPLACEABLE = Set.of(
            Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL,
            Blocks.STONE, Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY,
            Blocks.MYCELIUM, Blocks.SNOW_BLOCK);

    private static final BiomeTypeChanger PLAINS_TO_MOSS =
            new BiomeTypeChanger(SOURCE, MossHelper.TARGET);

    public PlainsToMoss() {
        addBS (Ring.TICK_10, PLAINS_TO_MOSS);                                    // REWRITE
        addPos(Ring.TICK_10, MossHelper.onlyMoss(PlainsToMoss::surface));        // SURFACE
        addPos(Ring.TICK_20, MossHelper.onlyMoss(MossHelper::lake));             // LAKE
        addPos(Ring.TICK_10, MossHelper.onlyMoss(MossHelper::deco));             // DECO
        addFinish(MossHelper.TREE_MOSS);                                          // DONE
    }

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel().getBiome(be.getBlockPos()).is(SOURCE);
    }

    // ───── 地表（清原版植物，铺苔藓块） ─────

    static void surface(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockState s = level.getBlockState(p);
        if (s.isAir()) return;
        if (REMOVE.contains(s.getBlock())) {
            set(level, p, Blocks.AIR.defaultBlockState());
            markDirty(level, p);
            return;
        }
        if (!REPLACEABLE.contains(s.getBlock())) return;
        if (level.getBlockState(p.above()).isAir()
                || level.getBlockState(p.below()).isAir()) {
            set(level, p, Blocks.MOSS_BLOCK.defaultBlockState());
            markDirty(level, p);
        }
    }
}