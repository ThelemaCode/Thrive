package com.thelema.thrive.generic.blockentity.modifier.b.moss;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
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

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.*;

/**
 * 平原 → Moss 一次性转换。
 * <p>
 * 四个 Ring 并行推进，靠 {@code delay} 形成流水线：
 * REWRITE 无延迟，SURFACE/LAKE/DECO 各落后 {@link #DELAY} 环，
 * 保证它们走第 r 环时群系已被 REWRITE 改好，{@code onlyMoss} 判断有效。
 * 全部返回 true 时替换结构收尾。
 */
public class PlainsToMoss implements BiomeModifier {

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
            BiomeTypeChanger.of(SOURCE, MossHelper.TARGET);

    /**
     * 流水线延迟：每个阶段落后上一阶段几环。
     */
    private static final int DELAY = 2;

    private static final Ring R_REWRITE = new Ring(10);
    private static final Ring R_SURFACE = new Ring(10, 0, 0, DELAY);
    private static final Ring R_LAKE = new Ring(20, 0, 0, DELAY * 2);
    private static final Ring R_DECO = new Ring(10, 0, 0, DELAY * 3);

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel() instanceof ServerLevel sl && sl.getBiome(be.getBlockPos()).is(SOURCE);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean rewrite = forBS(be, R_REWRITE, PLAINS_TO_MOSS);
        boolean surface = forPos(be, R_SURFACE, MossHelper.onlyMoss(PlainsToMoss::surface));
        boolean lake = forPos(be, R_LAKE, MossHelper.onlyMoss(MossHelper::lake));
        boolean deco = forPos(be, R_DECO, MossHelper.onlyMoss(MossHelper::deco));

        boolean all = rewrite & surface & lake & deco;
        if (all) placeTree((ServerLevel) be.getLevel(), be, MossHelper.TREE_MOSS);
        return all;
    }

    // ───── 地表（清原版植物，铺苔藓块） ─────

    static void surface(BiomeCoreBlockEntity be, BlockPos p) {
        if (be.getLevel() instanceof ServerLevel sl) {

            BlockState s = sl.getBlockState(p);
            if (s.isAir()) return;
            if (REMOVE.contains(s.getBlock())) {
                set(sl, p, Blocks.AIR.defaultBlockState());
                markDirty(sl, p);
                return;
            }
            if (!REPLACEABLE.contains(s.getBlock())) return;
            if (sl.getBlockState(p.above()).isAir()
                    || sl.getBlockState(p.below()).isAir()) {
                set(sl, p, Blocks.MOSS_BLOCK.defaultBlockState());
                markDirty(sl, p);
            }
        }
    }
}