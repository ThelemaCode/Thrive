package com.thelema.thrive.generic.blockentity.modifier.cloud;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.PhasedModifier;
import com.thelema.thrive.generic.blockentity.modifier.Scattered;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.*;

/**
 * 天空 → 云。
 * <p>
 * 在 BE 的 chunk 覆盖范围（由 {@code be.chunkRadius} 决定）内、
 * y 偏移 ∈ [-4, -2] 的盒里随机撒点，每点生成一朵随机尺寸的云。
 * 每朵 50% 概率是 cloud，50% 是 wispy_cloud。放置时跳过群系核心。
 */
public class SkyToCloud extends PhasedModifier {

    /** 每批间隔（tick）。20 = 1 秒。 */
    static final int INTERVAL = 20;
    /** 每批撒几点（= 几朵云）。 */
    static final int COUNT = 10;
    /** 共几批。10 × 18 = 180 朵。 */
    static final int PATCH = 18;

    /** 撒点 y 偏移范围（含两端）。 */
    static final int Y_MIN = -4, Y_MAX = -2;
    /** 云尺寸范围（含两端）：x/z ∈ [4,16]，y ∈ [2,3]。 */
    static final int SIZE_XZ_MIN = 4, SIZE_XZ_MAX = 16;
    static final int SIZE_Y_MIN = 2, SIZE_Y_MAX = 3;

    public SkyToCloud() {
        addPhase(SkyToCloud::generate);
    }

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return true;   // Sky 群系不存在，先不限制
    }

    private static boolean generate(BiomeCoreBlockEntity be) {
        BlockPos base = be.getBlockPos();
        int cr = 4;

        int ox1 = minX(base, cr) - base.getX();
        int ox2 = maxX(base, cr) - base.getX();
        int oz1 = minZ(base, cr) - base.getZ();
        int oz2 = maxZ(base, cr) - base.getZ();

        Scattered spots = Scattered.of(ox1, Y_MIN, oz1, ox2, Y_MAX, oz2, INTERVAL, COUNT, PATCH);
        return forPos(be, spots, SkyToCloud::place);
    }

    /** 以 p 为中心填随机尺寸的云，只填空气，跳过 BIOME_CORE。云/薄云各半。 */
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
            if (!cur.isAir()) continue;                    // 只填空气
            if (cur.is(BlockRegister.BIOME_CORE.get())) continue;   // 不覆盖核心
            set(level, q, s);
            markDirty(level, q);
        }
    }
}