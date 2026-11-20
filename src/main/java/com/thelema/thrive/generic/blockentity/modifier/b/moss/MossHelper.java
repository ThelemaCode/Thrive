package com.thelema.thrive.generic.blockentity.modifier.b.moss;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.PosApplier;
import com.thelema.thrive.generic.feature.Lake;
import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.FluidRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;


final class MossHelper {

    private MossHelper() {}

    static final ResourceKey<Biome> TARGET = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath("thrive", "moss"));
    static final ResourceLocation TREE_MOSS =
            ResourceLocation.fromNamespaceAndPath("thrive", "biome_core_tree_moss");

    /** moss_vegetation 权重表：苔藓地毯 25、短草 50、高草 10、杜鹃 7、开花杜鹃 4。 */
    static final Block[] VEGETATION = {
            Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET,
            Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET,
            Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET,
            Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET,
            Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET,
            Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET, Blocks.MOSS_CARPET,
            Blocks.MOSS_CARPET,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.SHORT_GRASS, Blocks.SHORT_GRASS,
            Blocks.TALL_GRASS, Blocks.TALL_GRASS, Blocks.TALL_GRASS, Blocks.TALL_GRASS,
            Blocks.TALL_GRASS, Blocks.TALL_GRASS, Blocks.TALL_GRASS, Blocks.TALL_GRASS,
            Blocks.TALL_GRASS, Blocks.TALL_GRASS,
            Blocks.AZALEA, Blocks.AZALEA, Blocks.AZALEA, Blocks.AZALEA,
            Blocks.AZALEA, Blocks.AZALEA, Blocks.AZALEA,
            Blocks.FLOWERING_AZALEA, Blocks.FLOWERING_AZALEA,
            Blocks.FLOWERING_AZALEA, Blocks.FLOWERING_AZALEA
    };

    // ───── 群系过滤（唯一的群系判断入口） ─────

    /** 跳过已是 moss 的坐标。扩张类 modifier 用。 */
    static PosApplier skipMoss(PosApplier inner) {
        return (be, p) -> {
            if (be.getLevel().getBiome(p).is(TARGET)) return;
            inner.apply(be, p);
        };
    }

    /** 只处理已是 moss 的坐标。一次转换后造景用。 */
    static PosApplier onlyMoss(PosApplier inner) {
        return (be, p) -> {
            if (!be.getLevel().getBiome(p).is(TARGET)) return;
            inner.apply(be, p);
        };
    }

    // ───── 地形判断 ─────

    static boolean isNaturalTerrain(BlockState s) {
        Block b = s.getBlock();
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND)
                || s.is(BlockTags.BASE_STONE_OVERWORLD)
                || s.is(BlockTags.BASE_STONE_NETHER)
                || s.is(BlockTags.TERRACOTTA)
                || b == Blocks.END_STONE
                || s.is(BlockTags.SNOW) || s.is(BlockTags.ICE)
                || b == Blocks.GRAVEL || b == Blocks.CLAY || b == Blocks.MUD
                || b == Blocks.CALCITE || b == Blocks.DRIPSTONE_BLOCK
                || b == Blocks.MAGMA_BLOCK || b == Blocks.OBSIDIAN
                || b == Blocks.SOUL_SAND || b == Blocks.SOUL_SOIL
                || b == Blocks.BONE_BLOCK || b == Blocks.SCULK
                || b == Blocks.SCULK_CATALYST;
    }

    // ───── 放湖 ─────

    /** 在 moss 表面随机放一盏灯笼莓汁湖。群系判断由调用方包装。 */
    static void lake(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        assert level != null;
        if (!level.getBlockState(p).isAir()) return;
        if (!level.getBlockState(p.below()).is(Blocks.MOSS_BLOCK)) return;
        if (level.random.nextInt(4) != 0) return;
        Lake.placeAt(level, p, level.random,
                FluidRegister.LANTERN_BERRY_JUICE_BLOCK.get().defaultBlockState(),
                Blocks.MOSS_BLOCK.defaultBlockState());
    }

    // ───── 造景 ─────

    /** 纯工具：不做群系判断，调用方决定是否处理当前点。 */
    static void deco(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        if (!level.getBlockState(p).isAir()) return;

        RandomSource random = level.random;
        BlockState above = level.getBlockState(p.above());
        BlockState below = level.getBlockState(p.below());

        if (below.is(Blocks.MOSS_BLOCK)) {
            if (random.nextInt(72) == 0) placeTree(level, random, p);
            else placeVegetation(level, random, p);
        } else if (above.is(Blocks.MOSS_BLOCK)) {
            if (random.nextInt(5) == 0) placeCaveVine(level, random, p);
        }
    }

    static void placeTree(ServerLevel level, RandomSource random, BlockPos p) {
        var feature = level.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceKey.create(Registries.CONFIGURED_FEATURE,
                        ResourceLocation.withDefaultNamespace("azalea_tree")));
        if (feature == null) return;
        feature.place(level, level.getChunkSource().getGenerator(), random, p);
    }

    static void placeVegetation(ServerLevel level, RandomSource random, BlockPos p) {
        Block veg = VEGETATION[random.nextInt(VEGETATION.length)];

        if (veg == Blocks.TALL_GRASS) {
            BlockPos above = p.above();
            if (!level.getBlockState(above).isAir()) return;
            level.setBlock(p, Blocks.TALL_GRASS.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(above, Blocks.TALL_GRASS.defaultBlockState()
                    .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            return;
        }
        level.setBlock(p, veg.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    static void placeCaveVine(ServerLevel level, RandomSource random, BlockPos ceiling) {
        int length = 1 + random.nextInt(8);
        boolean berries = random.nextInt(5) == 0;
        BlockPos last = ceiling;

        for (int i = 0; i < length; i++) {
            BlockPos pos = ceiling.below(i);
            if (!level.getBlockState(pos).isAir()) break;
            BlockState vine = (i == 0 ? Blocks.CAVE_VINES_PLANT : Blocks.CAVE_VINES)
                    .defaultBlockState()
                    .setValue(CaveVines.BERRIES, berries);
            level.setBlock(pos, vine, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            last = pos;
        }

        if (random.nextInt(3) == 0) {
            BlockPos below = last.below();
            if (level.getBlockState(below).isAir()) {
                level.setBlock(below, BlockRegister.LANTERN_BERRY.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }
}