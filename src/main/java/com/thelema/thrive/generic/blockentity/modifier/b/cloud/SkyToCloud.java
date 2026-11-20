package com.thelema.thrive.generic.blockentity.modifier.b.cloud;

import com.thelema.thrive.generic.block.Cloud;
import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.generic.blockentity.modifier.Scattered;
import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.*;
import net.minecraft.world.level.block.Block;

/**
 * 天空 → 云（转化器）。
 * <p>
 * 启动条件：主世界 y >= 200。
 * 撒云 + 改群系为 cloud，两者都完成后替换结构。
 */
public class SkyToCloud implements BiomeModifier {

    static final int INTERVAL = 20;
    static final int COUNT = 20;
    static final int PATCH = 18;

    static final int Y_MIN = -4, Y_MAX = -2;
    static final int SIZE_XZ_MIN = 4, SIZE_XZ_MAX = 16;
    static final int SIZE_Y_MIN = 2, SIZE_Y_MAX = 3;

    static final ResourceLocation TREE_CLOUD =
            ResourceLocation.fromNamespaceAndPath("thrive", "biome_core_tree_cloud");

    private static final BiomeTypeChanger ANY_TO_CLOUD = BiomeTypeChanger.any(BiomeConstant.CLOUD);
    private static final Ring R_REWRITE = new Ring(20);

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel)) return false;
        return be.getBlockPos().getY() >= 200;
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean cloud = clouds(be);
        boolean rewrite = forBS(be, R_REWRITE, ANY_TO_CLOUD);
        boolean all = cloud & rewrite;
        if (all) placeTree((ServerLevel) be.getLevel(), be, TREE_CLOUD);
        return all;
    }

    /** 撒云，包内可见，{@link Cloud} 复用。 */
    static boolean clouds(BiomeCoreBlockEntity be) {
        BlockPos base = be.getBlockPos();
        int cr = be.chunkRadius;
        int ox1 = minX(base, cr) - base.getX();
        int ox2 = maxX(base, cr) - base.getX();
        int oz1 = minZ(base, cr) - base.getZ();
        int oz2 = maxZ(base, cr) - base.getZ();
        Scattered spots = Scattered.of(ox1, Y_MIN, oz1, ox2, Y_MAX, oz2, INTERVAL, COUNT, PATCH);
        return forPos(be, spots, SkyToCloud::place);
    }

    static void place(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        RandomSource rng = level.random;
        BlockState s = (rng.nextBoolean()
                ? BlockRegister.CLOUD
                : BlockRegister.WISPY_CLOUD).get().defaultBlockState();

        int sx = SIZE_XZ_MIN + rng.nextInt(SIZE_XZ_MAX - SIZE_XZ_MIN + 1);
        int sy = SIZE_Y_MIN + rng.nextInt(SIZE_Y_MAX - SIZE_Y_MIN + 1);
        int sz = SIZE_XZ_MIN + rng.nextInt(SIZE_XZ_MAX - SIZE_XZ_MIN + 1);

        int x0 = -sx / 2, x1 = x0 + sx - 1;
        int y0 = -sy / 2, y1 = y0 + sy - 1;
        int z0 = -sz / 2, z1 = z0 + sz - 1;

        for (BlockPos q : BlockPos.betweenClosed(p.offset(x0, y0, z0), p.offset(x1, y1, z1))) {
            BlockState cur = level.getBlockState(q);
            if (!cur.isAir()) continue;
            if (cur.is(BlockRegister.BIOME_CORE.get())) continue;
            level.setBlock(q, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}