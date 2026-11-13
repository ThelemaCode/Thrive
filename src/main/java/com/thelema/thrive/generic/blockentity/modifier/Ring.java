package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class Ring implements PosFinder {

    /**
     * chunk 半径。范围 (2×CHUNK_RADIUS+1)³ chunk。
     */
    private static final int CHUNK_RADIUS = 4;


    private static final int MAX_RING = CHUNK_RADIUS * 16 + 15;

    /**
     * 1 秒 1 环。
     */
    public static final Ring TICK_20 = new Ring(20, MAX_RING, 0, 0, 0);
    /**
     * 0.5 秒 1 环。
     */
    public static final Ring TICK_10 = new Ring(10, MAX_RING, 0, 0, 0);

    private final int interval;
    private final int maxRing;
    private final int offsetX;
    private final int offsetZ;
    private final int delay;

    public Ring(int interval, int maxRing, int offsetX, int offsetZ, int delay) {
        this.interval = interval;
        this.maxRing = maxRing;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.delay = delay;
    }

    @Override
    public boolean find(BiomeCoreBlockEntity be, PosApplier apply) {
        long tick = BiomeUtil.getTick(be);
        if (tick % interval != 0) return false;
        int r = (int) (tick / interval) - delay;
        if (r > maxRing) return true;
        ring(be, r, apply);
        return r == maxRing;
    }

    /**
     * 遍历第 r 环（切比雪夫壳）。中心为 BE 位置偏移 (offsetX, 0, offsetZ)。
     */
    private void ring(BiomeCoreBlockEntity be, int r, PosApplier apply) {
        if (r < 0) return;
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockPos base = be.getBlockPos().offset(offsetX, 0, offsetZ);
        int cx = base.getX(), cz = base.getZ();

        int minX = BiomeUtil.minX(base, CHUNK_RADIUS);
        int maxX = BiomeUtil.maxX(base, CHUNK_RADIUS);
        int minZ = BiomeUtil.minZ(base, CHUNK_RADIUS);
        int maxZ = BiomeUtil.maxZ(base, CHUNK_RADIUS);
        int y0 = Math.max(BiomeUtil.minY(base, CHUNK_RADIUS), level.getMinBuildHeight());
        int y1 = Math.min(BiomeUtil.maxY(base, CHUNK_RADIUS), level.getMaxBuildHeight() - 1);

        if (r == 0) {
            if (cx < minX || cx > maxX || cz < minZ || cz > maxZ) return;
            for (int y = y0; y <= y1; y++) apply.apply(be, new BlockPos(cx, y, cz));
            return;
        }

        // 上下两条边：x ∈ [cx-r, cx+r] ∩ [minX, maxX]，z = cz±r
        int xLo = Math.max(cx - r, minX);
        int xHi = Math.min(cx + r, maxX);
        if (xLo <= xHi) {
            int zN = cz - r, zP = cz + r;
            boolean zNOk = zN >= minZ && zN <= maxZ;
            boolean zPOk = zP >= minZ && zP <= maxZ;
            for (int x = xLo; x <= xHi; x++)
                for (int y = y0; y <= y1; y++) {
                    if (zNOk) apply.apply(be, new BlockPos(x, y, zN));
                    if (zPOk) apply.apply(be, new BlockPos(x, y, zP));
                }
        }

        // 左右两条边：z ∈ [cz-r+1, cz+r-1] ∩ [minZ, maxZ]，x = cx±r
        int zLo = Math.max(cz - r + 1, minZ);
        int zHi = Math.min(cz + r - 1, maxZ);
        if (zLo <= zHi) {
            int xN = cx - r, xP = cx + r;
            boolean xNOk = xN >= minX && xN <= maxX;
            boolean xPOk = xP >= minX && xP <= maxX;
            for (int z = zLo; z <= zHi; z++)
                for (int y = y0; y <= y1; y++) {
                    if (xNOk) apply.apply(be, new BlockPos(xN, y, z));
                    if (xPOk) apply.apply(be, new BlockPos(xP, y, z));
                }
        }
    }
}