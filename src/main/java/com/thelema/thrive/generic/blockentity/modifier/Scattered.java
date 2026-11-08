package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * 空间随机撒点 PosFinder。
 * <p>
 * 在 BE 中心偏移 {@code [ox1,ox2]×[oy1,oy2]×[oz1,oz2]} 的盒内随机撒点。
 * 每 {@code interval} tick 撒 {@code count} 个点，共 {@code patch} 批，
 * 总撒点 {@code count×patch}，总耗时约 {@code interval×patch} tick。
 * <p>
 * 进度记在 BE 的 tick 里，第 {@code patch} 批完成后返回 true。
 * 每批用 {@code (BE 坐标, batchIndex)} 派生随机种子，保证可重现、批次间不重叠。
 * 无状态，可 static final 复用。
 */
public final class Scattered implements PosFinder {

    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final int interval;
    private final int count;
    private final int patch;

    private Scattered(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                      int interval, int count, int patch) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.interval = interval;
        this.count = count;
        this.patch = patch;
    }

    /**
     * @param ox1,oy1,oz1 盒的一个角（相对 BE 中心）
     * @param ox2,oy2,oz2 盒的另一个角（相对 BE 中心，与前者顺序无关）
     * @param interval    每多少 tick 撒一批。1 = 每 tick。
     * @param count       每批撒几个点。
     * @param patch       共撒几批。
     */
    public static Scattered of(int ox1, int oy1, int oz1,
                               int ox2, int oy2, int oz2,
                               int interval, int count, int patch) {
        return new Scattered(
                Math.min(ox1, ox2), Math.min(oy1, oy2), Math.min(oz1, oz2),
                Math.max(ox1, ox2), Math.max(oy1, oy2), Math.max(oz1, oz2),
                Math.max(1, interval), Math.max(1, count), Math.max(1, patch));
    }

    @Override
    public boolean find(BiomeCoreBlockEntity be, PosApplier apply) {
        long tick = BiomeUtil.getTick(be);
        long batchIndex = tick / interval;
        if (batchIndex >= patch) return true;
        if (tick % interval != 0) return false;

        BlockPos c = be.getBlockPos();
        Level level = be.getLevel();
        int yMin = level != null ? level.getMinBuildHeight() : c.getY() + minY;
        int yMax = level != null ? level.getMaxBuildHeight() - 1 : c.getY() + maxY;

        // 种子 = (BE 坐标, batchIndex)。同一批重入结果一致，不同批不重叠
        RandomSource rng = RandomSource.create(c.asLong() ^ (batchIndex * 0x9E3779B97F4A7C15L));

        int spanX = maxX - minX + 1;
        int spanY = maxY - minY + 1;
        int spanZ = maxZ - minZ + 1;

        for (int i = 0; i < count; i++) {
            int dx = minX + rng.nextInt(spanX);
            int dy = minY + rng.nextInt(spanY);
            int dz = minZ + rng.nextInt(spanZ);
            int y = Math.max(yMin, Math.min(yMax, c.getY() + dy));
            apply.apply(be, new BlockPos(c.getX() + dx, y, c.getZ() + dz));
        }
        return batchIndex + 1 >= patch;
    }
}