package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/**
 * 射线检测：从范围的一面垂直射进去，把沿途的**表面点**交给 applier。
 *
 * <p>比 {@link Ring} 便宜几个数量级：Ring 遍历整个体积，Ray 只走「一面 → 另一面」这一条线。
 * 它是粗筛，射中的点还要过 applier 的判定。
 *
 * <h3>命中语义</h3>
 * 命中 = **由空气进入非空气的那一格**；连续的非空气块只算第一个（一面墙算一个点，不算十个）。
 * 一条射线最多收 {@code hitCount} 个，之后直接结束。
 * <ul>
 *   <li>{@code hitCount = 1}：只拿最外层——裸露的草方块、裸露的树叶。</li>
 *   <li>{@code hitCount = 2}：再加一层——树叶缝隙下方的地面、草方块下面洞穴的顶面。</li>
 * </ul>
 * 射线方向由 {@code direction} 决定，起点在**它的反面**；走到正方体另一面就停。
 *
 * <h3>节奏与分布</h3>
 * 每 {@code interval} tick 射一批，每批 {@code count} 条，共 {@code batch} 批。
 * 面上的位置用 <b>R2 低差异序列</b>：第 k 个点落在 {@code frac(0.5 + k·α)}，不需要任何内存，
 * 点自己铺开、批与批天然穿插；再按 BE 坐标给一套固定的随机平移，让每个核心花纹不同但可复现。
 *
 * <p>整个遍历只复用两个 {@link BlockPos.MutableBlockPos}，每命中一个点才产生一个 {@code BlockPos}。
 */
public final class Ray implements PosFinder {

    /** R2 序列的两个生成元（最速可扩展点集的黄金比推广）。 */
    private static final double R2_A1 = 0.7548776662466927D;
    private static final double R2_A2 = 0.5698402909980532D;

    private final Direction direction;
    private final int hitCount;
    private final int interval;
    private final int count;
    private final int batch;

    /**
     * @param direction 射线前进的方向（起点在它的反面）
     * @param hitCount  一条射线最多收几个表面点
     * @param interval  每几 tick 射一批
     * @param count     每批几条射线
     * @param batch     一共射几批
     */
    public Ray(Direction direction, int hitCount, int interval, int count, int batch) {
        this.direction = direction;
        this.hitCount = Math.max(1, hitCount);
        this.interval = Math.max(1, interval);
        this.count = Math.max(1, count);
        this.batch = Math.max(1, batch);
    }

    @Override
    public boolean find(BiomeCoreBlockEntity be, PosApplier apply) {
        if (!(be.getLevel() instanceof ServerLevel level)) return false;

        long tick = BiomeUtil.getTick(be);
        long index = tick / interval;
        if (index >= batch) return true;
        if (tick % interval != 0) return false;

        BlockPos center = be.getBlockPos();
        int cr = be.chunkRadius;
        int minX = BiomeUtil.minX(center, cr), maxX = BiomeUtil.maxX(center, cr);
        int minZ = BiomeUtil.minZ(center, cr), maxZ = BiomeUtil.maxZ(center, cr);
        int minY = Math.max(BiomeUtil.minY(center, cr), level.getMinBuildHeight());
        int maxY = Math.min(BiomeUtil.maxY(center, cr), level.getMaxBuildHeight() - 1);
        if (minX > maxX || minZ > maxZ || minY > maxY) return true;

        // 三个轴按序号排好：法线轴 a，另外两个是面上的自由轴
        int[] lo = {minX, minY, minZ};
        int[] hi = {maxX, maxY, maxZ};
        int a = direction.getAxis().ordinal();
        int u = (a + 1) % 3;
        int v = (a + 2) % 3;

        // 起点在 direction 的反面；步数 = 穿过整个正方体
        int fixed = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? lo[a] : hi[a];
        int maxSteps = hi[a] - lo[a] + 1;
        int uSpan = hi[u] - lo[u] + 1;
        int vSpan = hi[v] - lo[v] + 1;

        int worldMinY = level.getMinBuildHeight();
        int worldMaxY = level.getMaxBuildHeight();

        // 每个核心一套固定的随机平移：图案不同，但可复现
        RandomSource rng = RandomSource.create(center.asLong());
        double shiftU = rng.nextDouble();
        double shiftV = rng.nextDouble();

        int[] c = new int[3];
        BlockPos.MutableBlockPos start = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < count; i++) {
            long k = index * count + i;
            int pu = lo[u] + Math.min(uSpan - 1, (int) (frac(0.5 + k * R2_A1 + shiftU) * uSpan));
            int pv = lo[v] + Math.min(vSpan - 1, (int) (frac(0.5 + k * R2_A2 + shiftV) * vSpan));

            c[a] = fixed;
            c[u] = pu;
            c[v] = pv;
            start.set(c[0], c[1], c[2]);

            // 沿 direction 推进：命中 = 由空气进入非空气的那一格
            pos.set(start);
            boolean prevAir = true;
            int hits = 0;
            for (int step = 0; step < maxSteps; step++) {
                int y = pos.getY();
                if (y < worldMinY || y >= worldMaxY) break;
                if (!level.isLoaded(pos)) break;

                if (level.getBlockState(pos).isAir()) {
                    prevAir = true;
                } else if (prevAir) {
                    apply.apply(be, pos.immutable());
                    prevAir = false;
                    if (++hits >= hitCount) break;
                }
                pos.move(direction);
            }
        }

        return index + 1 >= batch;
    }

    private static double frac(double x) {
        return x - Math.floor(x);
    }
}
