package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * 空间随机撒点 PosFinder。
 * <p>
 * 与 {@link Ring} 的分工：Ring 按切比雪夫壳顺序遍历全部坐标，适合必须逐点判断的效果；
 * Scattered 只撒固定密度的采样点，适合"撒种子"的效果（云团、随机植被），
 * 跳过大量无关坐标，代价是可能漏检。
 * <p>
 * 撒点总数 = volume / density，每 tick 撒 batch 个，进度记在 BE 的 tick 里。
 * 每批用 {@code (BE 坐标, tick)} 派生随机种子，保证可重现、不依赖全局随机源。
 * 无状态，可 static final 复用。
 */
public final class Scattered implements PosFinder {

    /** 默认密度：平均每 64 格体积（4×4×4）撒一个点。 */
    static final int DEFAULT_DENSITY = 64;
    /** 默认批次：每 tick 撒 256 个点。 */
    static final int DEFAULT_BATCH = 256;

    private final int hx, hy, hz;
    private final long total;
    private final int batch;

    private Scattered(int hx, int hy, int hz, long total, int batch) {
        this.hx = hx;
        this.hy = hy;
        this.hz = hz;
        this.total = total;
        this.batch = batch;
    }

    /** 在 BE 中心 ±(x,y,z) 的盒内随机撒点，密度与批次用默认值。 */
    public static Scattered of(int x, int y, int z) {
        return of(x, y, z, DEFAULT_DENSITY, DEFAULT_BATCH);
    }

    /**
     * @param density 平均每多少格体积撒一个点。越大越稀。
     * @param batch   每 tick 撒点上限。越大越快完成。
     */
    public static Scattered of(int x, int y, int z, int density, int batch) {
        long volume = (2L * x + 1) * (2L * y + 1) * (2L * z + 1);
        long total = Math.max(1L, volume / Math.max(1, density));
        return new Scattered(x, y, z, total, Math.max(1, batch));
    }

    @Override
    public boolean find(BiomeCoreBlockEntity be, PosApplier apply) {
        long tick = BiomeUtil.getTick(be);
        long done = tick * batch;
        if (done >= total) return true;

        int n = (int) Math.min(batch, total - done);
        BlockPos c = be.getBlockPos();
        Level level = be.getLevel();
        int yMin = level != null ? level.getMinBuildHeight() : c.getY() - hy;
        int yMax = level != null ? level.getMaxBuildHeight() - 1 : c.getY() + hy;

        // 种子 = (中心坐标, tick)。同一 tick 重入结果一致，不同 tick 批次不重叠
        RandomSource rng = RandomSource.create(c.asLong() ^ (tick * 0x9E3779B97F4A7C15L));

        for (int i = 0; i < n; i++) {
            int dx = rng.nextInt(2 * hx + 1) - hx;
            int dy = rng.nextInt(2 * hy + 1) - hy;
            int dz = rng.nextInt(2 * hz + 1) - hz;
            int y = Math.max(yMin, Math.min(yMax, c.getY() + dy));
            apply.apply(be, new BlockPos(c.getX() + dx, y, c.getZ() + dz));
        }
        return done + n >= total;
    }
}