package com.thelema.thrive.generic.blockentity.modifier.c.sunberry;

import com.thelema.thrive.generic.block.SunBerry;
import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.PhasedModifier;
import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.FluidRegister;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;


public class MossToSunBerry extends PhasedModifier {

    static final int TREE_FREE_RADIUS = 7;

    static final ResourceKey<Biome> SOURCE = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath("thrive", "moss"));
    static final ResourceKey<Biome> TARGET = BiomeConstant.SUNBERRY;
    static final ResourceLocation TREE_SUNBERRY =
            ResourceLocation.fromNamespaceAndPath("thrive", "biome_core_tree_sunberry");

    static final int LAKE_TILE = 32;
    static final int LAKE_INNER = 20;
    static final int LAKE_DEPTH = 3;

    static final int SUN_BERRY_CHANCE = 512;
    static final int LANTERN_BERRY_CHANCE = 64;

    /** 湖列 → 挖洞前的 topY。phase 3 写、phase 4 读。 */
    private final Map<Long, Integer> lakeTop = new HashMap<>();

    private static final BiomeTypeChanger MOSS_TO_SUN_BERRY = BiomeTypeChanger.of(SOURCE, TARGET);

    public MossToSunBerry() {
        //addPhase(be -> forBS(be, Ring.TICK_20, MOSS_TO_SUN_BERRY));             // REWRITE
        //addPhase(MossToSunBerry::clear);        // CLEAR
        //addPhase(MossToSunBerry::surface);      // SURFACE
        //addPhase(this::lakeDig);                // LAKE_DIG
        //addPhase(this::lakeFill);               // LAKE_FILL
        //addPhase(this::plant);                  // PLANT
        //addPhase(be -> { placeTree((ServerLevel) be.getLevel(), be, TREE_SUNBERRY); return true; });                           // DONE
    }

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel().getBiome(be.getBlockPos()).is(SOURCE);
    }

    static Holder<Biome> target(ServerLevel level) {
        return level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(TARGET);
    }

    static boolean withinFreeRadius(BiomeCoreBlockEntity be, BlockPos p) {
        BlockPos c = be.getBlockPos();
        return Math.max(Math.abs(p.getX() - c.getX()), Math.abs(p.getZ() - c.getZ())) <= TREE_FREE_RADIUS;
    }

    // ─── 阶段 1：清理 ───

    static void clear(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockState s = level.getBlockState(p);
        if (s.isAir()) return;
        if (!isMossOrSunberry(level, p)) return;

        boolean oak = s.is(Blocks.OAK_LOG) || s.is(Blocks.OAK_WOOD)
                || s.is(Blocks.DARK_OAK_LOG) || s.is(Blocks.DARK_OAK_WOOD)
                || s.is(Blocks.OAK_LEAVES) || s.is(Blocks.DARK_OAK_LEAVES);
        boolean azalea = s.is(Blocks.AZALEA_LEAVES) || s.is(Blocks.FLOWERING_AZALEA_LEAVES)
                || s.is(Blocks.AZALEA) || s.is(Blocks.FLOWERING_AZALEA);
        boolean plant = s.is(BlockTags.SAPLINGS)
                || s.is(BlockTags.FLOWERS)
                || s.is(BlockTags.REPLACEABLE_BY_TREES)
                || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)
                || s.is(Blocks.MOSS_CARPET)
                || s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT)
                || s.is(BlockRegister.LANTERN_BERRY.get());
        if (!oak && !azalea && !plant) return;

        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    // ─── 阶段 2：地表 ───

    static void surface(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        if (!isMossOrSunberry(level, p)) return;
        BlockState s = level.getBlockState(p);

        if (!s.getFluidState().isEmpty()) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            return;
        }
        if (!s.is(Blocks.GRASS_BLOCK) && !s.is(BlockTags.DIRT) && !s.is(Blocks.MOSS_BLOCK)) return;

        boolean sideExposed = false;
        for (Direction d : Direction.values()) {
            if (level.getBlockState(p.relative(d)).isAir()) {
                sideExposed = true;
                break;
            }
        }
        int topY = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, p).getY();
        boolean nearSurface = p.getY() >= topY - 4 && p.getY() <= topY;
        if (!sideExposed && !nearSurface) return;

        level.setBlock(p, BlockRegister.LANTERN_BERRY_PEEL.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    // ─── 阶段 3：挖湖 ───

    void lakeDig(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        if (withinFreeRadius(be, p)) return;
        if (!isLakePos(p)) return;
        if (!isMossOrSunberry(level, p)) return;
        long col = ((long) p.getX() << 32) | (p.getZ() & 0xFFFFFFFFL);
        if (lakeTop.containsKey(col)) return;
        int topY = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, p).getY();
        lakeTop.put(col, topY);
        for (int y = topY - 1; y >= topY - LAKE_DEPTH; y--) {
            BlockPos q = new BlockPos(p.getX(), y, p.getZ());
            level.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    // ─── 阶段 4：补壁 + 灌汁 ───

    void lakeFill(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        if (withinFreeRadius(be, p)) return;
        if (!isLakePos(p)) return;
        long col = ((long) p.getX() << 32) | (p.getZ() & 0xFFFFFFFFL);
        Integer topY = lakeTop.get(col);
        if (topY == null) return;
        if (p.getY() != topY - 1) return;

        BlockState peel = BlockRegister.LANTERN_BERRY_PEEL.get().defaultBlockState();
        BlockState juice = FluidRegister.LANTERN_BERRY_JUICE_STILL.get()
                .defaultFluidState().createLegacyBlock();

        // 湖底
        BlockPos bottom = new BlockPos(p.getX(), topY - LAKE_DEPTH - 1, p.getZ());
        level.setBlock(bottom, peel, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);

        // 侧壁
        for (int y = topY - 1; y >= topY - LAKE_DEPTH; y--) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = new BlockPos(p.getX(), y, p.getZ()).relative(d);
                if (isLakePos(n)) continue;
                BlockState ns = level.getBlockState(n);
                if (ns.isAir() || !ns.getFluidState().isEmpty()) continue;
                level.setBlock(n, peel, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }

        // 灌汁：底壁已就位，此刻灌 source 方块
        for (int y = topY - 1; y >= topY - LAKE_DEPTH; y--) {
            BlockPos q = new BlockPos(p.getX(), y, p.getZ());
            level.setBlock(q, juice, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    // ─── 阶段 5：植被 ───

    void plant(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        if (withinFreeRadius(be, p)) return;
        if (!isMossOrSunberry(level, p)) return;
        BlockState s = level.getBlockState(p);
        if (!s.is(BlockRegister.LANTERN_BERRY_PEEL.get())) return;

        BlockPos above = p.above();
        if (!level.getBlockState(above).isAir()) return;
        if (!level.canSeeSky(above)) return;

        RandomSource random = level.random;
        if (random.nextInt(SUN_BERRY_CHANCE) == 0) {
            if (SunBerry.tryPlaceSunBerry(level, above.above())) {
                return;
            }
        }
        if (random.nextInt(LANTERN_BERRY_CHANCE) == 0) {
            level.setBlock(above, BlockRegister.LANTERN_BERRY.get().defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    // ─── 公共 ───

    static boolean isLakePos(BlockPos p) {
        int lx = Math.floorMod(p.getX(), LAKE_TILE) - LAKE_TILE / 2;
        int lz = Math.floorMod(p.getZ(), LAKE_TILE) - LAKE_TILE / 2;
        if (Math.abs(lx) > LAKE_INNER / 2 || Math.abs(lz) > LAKE_INNER / 2) return false;

        int tx = Math.floorDiv(p.getX(), LAKE_TILE);
        int tz = Math.floorDiv(p.getZ(), LAKE_TILE);
        long seed = (long) tx * 0x9E3779B97F4A7C15L ^ (long) tz * 0xC2B2AE3D27D4EB4FL;
        seed ^= (seed >>> 33);
        seed *= 0xFF51AFD7ED558CCDL;
        seed ^= (seed >>> 33);
        return (seed & 3L) == 0;
    }

    static boolean isMossOrSunberry(ServerLevel level, BlockPos p) {
        Holder<Biome> biome = level.getBiome(p);
        return biome.is(SOURCE) || biome.is(TARGET);
    }
}