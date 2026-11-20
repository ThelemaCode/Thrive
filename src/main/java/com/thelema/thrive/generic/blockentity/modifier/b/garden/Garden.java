package com.thelema.thrive.generic.blockentity.modifier.b.garden;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.constant.TagConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;
import net.minecraft.world.level.block.Block;

/**
 * 花园扩散器：BE 已在 garden 群系，向外扩张。不替换结构。
 */
public class Garden implements BiomeModifier {

    private static final BiomeTypeChanger TO_GARDEN =
            BiomeTypeChanger.any(GardenHelper.TARGET);

    private static final int DELAY = 2;

    private static final Ring R_PREPARE   = new Ring(20, 0, 0, 0);
    private static final Ring R_GARDEN    = new Ring(20, 0, 0, DELAY);
    private static final Ring R_BONE_MEAL = new Ring(20, 0, 0, DELAY * 2);
    private static final Ring R_REWRITE   = new Ring(20, 0, 0, DELAY * 3);

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel().getBiome(be.getBlockPos()).is(GardenHelper.TARGET);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean prepare  = forPos(be, R_PREPARE,   GardenHelper.skipGarden(Garden::prepare));
        boolean garden   = forPos(be, R_GARDEN,    GardenHelper.skipGarden(GardenHelper::garden));
        boolean boneMeal = forPos(be, R_BONE_MEAL, GardenHelper.skipGarden(GardenHelper::boneMeal));
        boolean rewrite  = forBS (be, R_REWRITE,   TO_GARDEN);
        return prepare & garden & boneMeal & rewrite;
    }

    private static void prepare(BiomeCoreBlockEntity be, BlockPos p) {
        GardenHelper.clear(be, p);
        surface(be, p);
    }

    // ───── 地表改造 ─────

    static void surface(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockState s = level.getBlockState(p);
        if (s.isAir() || !s.getFluidState().isEmpty()) return;
        if (!s.is(TagConstant.NATURAL_SURFACE)) return;

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, p.getX(), p.getZ());
        if (p.getY() >= surfaceY - 1 && level.getBlockState(p.above()).isAir()) {
            level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            return;
        }

        if (hasAirSide(level, p)) {
            level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            return;
        }
        int depth = level.getHeight(Heightmap.Types.WORLD_SURFACE, p.getX(), p.getZ()) - p.getY();
        if (depth < 1 || depth > GardenHelper.SAND_DEPTH) return;
        if (s.is(BlockTags.SAND)) {
            level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        } else if (s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE)) {
            level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    static boolean hasAirSide(ServerLevel level, BlockPos p) {
        for (Direction d : Direction.Plane.HORIZONTAL)
            if (level.getBlockState(p.relative(d)).isAir()) return true;
        return false;
    }
}