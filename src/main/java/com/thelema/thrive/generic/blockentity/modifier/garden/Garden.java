package com.thelema.thrive.generic.blockentity.modifier.garden;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.PhasedModifier;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.markDirty;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.set;

public class Garden extends PhasedModifier {

    private static final BiomeTypeChanger TO_GARDEN = BiomeTypeChanger.any(GardenHelper.TARGET);

    public Garden() {
        addPos(Ring.TICK_20, Garden::prepare);           // PREPARE：clear + surface
        addPos(Ring.TICK_20, Garden::garden);            // GARDEN
        addPos(Ring.TICK_20, Garden::boneMeal);          // BONE_MEAL
        addBS(Ring.TICK_20, TO_GARDEN);                  // REWRITE
        addFinish(GardenHelper.TREE_GARDEN);             // DONE
    }

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel().getBiome(be.getBlockPos()).is(GardenHelper.TARGET);
    }

    // ── 三个阶段包装：跳过已是 garden 的地区，处理非花园（扩张） ──

    private static void prepare(BiomeCoreBlockEntity be, BlockPos p) {
        if (be.getLevel().getBiome(p).is(GardenHelper.TARGET)) return;
        GardenHelper.clear(be, p);
        surface(be, p);
    }

    private static void garden(BiomeCoreBlockEntity be, BlockPos p) {
        if (be.getLevel().getBiome(p).is(GardenHelper.TARGET)) return;
        GardenHelper.garden(be, p);
    }

    private static void boneMeal(BiomeCoreBlockEntity be, BlockPos p) {
        if (be.getLevel().getBiome(p).is(GardenHelper.TARGET)) return;
        GardenHelper.boneMeal(be, p);
    }

    // ───── 地表改造 ─────

    static void surface(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockState s = level.getBlockState(p);
        if (s.isAir() || !s.getFluidState().isEmpty()) return;
        if (!isNaturalTerrain(s)) return;

        // ① 上方空气 → 草
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, p.getX(), p.getZ());
        if (p.getY() >= surfaceY - 1 && level.getBlockState(p.above()).isAir()) {
            set(level, p, Blocks.GRASS_BLOCK.defaultBlockState());
            markDirty(level, p);
            return;
        }

        // ② 侧面裸漏 → 泥土
        if (hasAirSide(level, p)) {
            set(level, p, Blocks.DIRT.defaultBlockState());
            markDirty(level, p);
            return;
        }
        // ③ 深层沙/砂岩，仅地表下 SAND_DEPTH 格内
        int depth = level.getHeight(Heightmap.Types.WORLD_SURFACE, p.getX(), p.getZ()) - p.getY();
        if (depth < 1 || depth > GardenHelper.SAND_DEPTH) return;
        if (s.is(BlockTags.SAND)) {
            set(level, p, Blocks.DIRT.defaultBlockState());
            markDirty(level, p);
        } else if (s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE)) {
            set(level, p, Blocks.STONE.defaultBlockState());
            markDirty(level, p);
        }
    }

    static boolean hasAirSide(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.Plane.HORIZONTAL)
            if (level.getBlockState(p.relative(d)).isAir()) return true;
        return false;
    }

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
}