package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@EventBusSubscriber
public final class BiomeUtil {

    private BiomeUtil() {}

    /** 全局待重发 chunk，按维度隔离。 */
    private static final Map<ResourceKey<Level>, Set<Long>> BIOME_DIRTY = new ConcurrentHashMap<>();

    // =======================   生命周期   =====================================

    /** BE 统一入口，失败时推进 tick。 */
    public static boolean runTick(BiomeModifier modifier, BiomeCoreBlockEntity be) {
        boolean done = modifier.tick(be);
        if (!done) setTick(be, getTick(be) + 1);
        return done;
    }

    /** 读取转换进度。 */
    public static long getTick(BiomeCoreBlockEntity be) { return be.data.getLong("tick"); }

    /** 写入转换进度。 */
    public static void setTick(BiomeCoreBlockEntity be, long v) { be.data.putLong("tick", v); }


    // =======================   遍历   =====================================

    /** 单点遍历，逐 pos 回调。 */
    public static boolean forPos(BiomeCoreBlockEntity be, PosFinder finder, PosApplier apply) {
        return finder.find(be, apply);
    }

    /** 分区遍历，聚 chunk/section 后批量。 */
    public static boolean forBS(BiomeCoreBlockEntity be, PosFinder finder, SectionApplier apply) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return true;
        Map<Long, Map<Integer, Set<Integer>>> touched = new HashMap<>();
        boolean done = finder.find(be, (b, pos) -> {
            int si = (pos.getY() - sl.getMinBuildHeight()) >> 4;
            int idx = (((pos.getX() & 15) >> 2) * 4 + ((pos.getY() & 15) >> 2)) * 4 + ((pos.getZ() & 15) >> 2);
            touched.computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), x -> new HashMap<>())
                    .computeIfAbsent(si, x -> new HashSet<>()).add(idx);
        });
        for (Long chunkKey : touched.keySet()) markBiomeDirty(sl, chunkKey);
        for (var ce : touched.entrySet()) {
            ChunkPos cp = new ChunkPos(ce.getKey());
            ChunkAccess chunk = sl.getChunk(cp.x, cp.z, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            for (var se : ce.getValue().entrySet()) apply.apply(chunk, se.getKey(), se.getValue());
        }
        return done;
    }

    /** 跳过已是 key 群系的坐标 */
    public static PosApplier skip(ResourceKey<Biome> key,PosApplier inner) {
        return (be, p) -> {
            if (be.getLevel() instanceof ServerLevel sl && sl.getBiome(p).is(key)) return;
            inner.apply(be, p);
        };
    }

    /** 只处理已是 key 群系的坐标 */
    public static PosApplier only(ResourceKey<Biome> key,PosApplier inner) {
        return (be, p) -> {
            if (be.getLevel() instanceof ServerLevel sl && sl.getBiome(p).is(key)){
                inner.apply(be, p);
            }
        };
    }


    // =======================   群系数据同步   =====================================

    /** 改写 section 内命中 unit 的群系。 */
    static void applyBiomeSection(ChunkAccess chunk, int sectionIndex, Set<Integer> units,
                                  Holder<Biome> target, Predicate<Holder<Biome>> shouldReplace) {
        LevelChunkSection section = chunk.getSection(sectionIndex);
        var old = section.getBiomes();
        Holder<Biome>[] cache = new Holder[64];
        for (int j = 0; j < 4; j++)
            for (int k = 0; k < 4; k++)
                for (int l = 0; l < 4; l++)
                    cache[(j * 4 + k) * 4 + l] = old.get(j, k, l);

        int qx = chunk.getPos().getMinBlockX() >> 2;
        int qy = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex)) >> 2;
        int qz = chunk.getPos().getMinBlockZ() >> 2;

        section.fillBiomesFromNoise((x, y, z, s) -> {
            int j = x - qx;
            int k = y - qy;
            int l = z - qz;
            int idx = (j * 4 + k) * 4 + l;
            if (!units.contains(idx)) return cache[idx];
            return shouldReplace.test(cache[idx]) ? target : cache[idx];
        }, null, qx, qy, qz);

        chunk.setUnsaved(true);
    }

    /** 取当前维度的「群系待重发」集合。 */
    private static Set<Long> biomeDirtySet(ServerLevel level) {
        return BIOME_DIRTY.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());
    }

    /**
     * 标记某个 chunk 的群系变了、待重发。
     *
     * <p>只有群系改动需要它：方块改动走 {@code setBlock} 自带的 section 包，
     * 客户端会自己重建那一个 section。
     */
    private static void markBiomeDirty(ServerLevel level, long chunkKey) {
        biomeDirtySet(level).add(chunkKey);
    }

    /** 每 10 tick（0.5 秒）给每个维度发一个 chunk 的群系包。 */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 10 != 0) return;
        for (ServerLevel level : e.getServer().getAllLevels()) flushOne(level);
    }

    /**
     * 发一个 chunk 的群系更新，然后返回。
     *
     * <p>用 {@code ClientboundChunksBiomesPacket}（只含群系，不含方块与光照），
     * 但客户端收到后会把该 chunk 及周围 3×3 的所有 section 标脏重建——
     * 所以一次只发一个，81 个 chunk 约 40 秒铺完，玩家完全无感。
     *
     * <p>未加载的 chunk 留在集合里，等加载后再发。
     */
    public static void flushOne(ServerLevel level) {
        Set<Long> set = BIOME_DIRTY.get(level.dimension());
        if (set == null || set.isEmpty()) return;
        var iter = set.iterator();
        while (iter.hasNext()) {
            long key = iter.next();
            ChunkPos cp = new ChunkPos(key);
            ChunkAccess chunk = level.getChunk(cp.x, cp.z, ChunkStatus.FULL, false);
            if (!(chunk instanceof LevelChunk lc)) continue;   // 未加载，下轮再试
            iter.remove();
            level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(lc));
            return;
        }
    }

    /** 重算 center 周围 chunkRadius 范围内所有已加载 chunk 的光照。 */
    public static void relightArea(ServerLevel level, BlockPos center, int chunkRadius) {
        LevelLightEngine light = level.getLightEngine();
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++)
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos cp = new ChunkPos(cx + dx, cz + dz);
                if (!level.hasChunk(cp.x, cp.z)) continue;
                light.setLightEnabled(cp, false);
                light.setLightEnabled(cp, true);
            }
    }

    // =======================   结构替换   =====================================

    /** 从模板找 BiomeCore 相对坐标。 */
    public static BlockPos findCoreOffset(StructureTemplate template) {
        List<StructureTemplate.StructureBlockInfo> list = template.filterBlocks(
                BlockPos.ZERO, new StructurePlaceSettings(), BlockRegister.BIOME_CORE.get());
        return list.isEmpty() ? null : list.get(0).pos();
    }

    /**
     * 用结构替换 BE，核心对齐，保留地表。
     */
    public static void placeTree(ServerLevel sl, BiomeCoreBlockEntity be, ResourceLocation toId) {
        StructureTemplate to = sl.getStructureManager().get(toId).orElse(null);
        if (to == null) return;
        BlockPos corePos = be.getBlockPos();
        BlockPos toCore = findCoreOffset(to);
        if (toCore == null) return;
        BlockPos toOrigin = corePos.subtract(toCore);

        // 材料消耗前，有容器残留的（如水桶→空桶）原地掉落
        for (ItemStack stack : be.rotator.items) {
            ItemStack remainder = stack.getCraftingRemainingItem();
            if (!remainder.isEmpty()) {
                Block.popResource(sl, corePos.above(), remainder.copy());
            }
        }

        be.rotator.clear();   // 材料直接消耗，不保存

        to.placeInWorld(sl, toOrigin, toOrigin, new StructurePlaceSettings()
                        .setRotation(Rotation.getRandom(sl.random))
                        .setMirror(Mirror.NONE)
                        .setRotationPivot(toCore)
                        .setIgnoreEntities(false).addProcessor(BlockIgnoreProcessor.AIR),
                sl.random, 50);

    }

    // =======================   边界判断   =====================================

    /** 中心 section/chunk 的最小坐标。 */
    private static int min(BlockPos center, int chunkRadius, int axis) {
        return ((coord(center, axis) >> 4) - chunkRadius) << 4;
    }

    /** 中心 section/chunk 的最大坐标（含末格）。 */
    private static int max(BlockPos center, int chunkRadius, int axis) {
        return (((coord(center, axis) >> 4) + chunkRadius) << 4) + 15;
    }

    private static int coord(BlockPos p, int axis) {
        return switch (axis) {
            case 0 -> p.getX();
            case 1 -> p.getY();
            default -> p.getZ();
        };
    }

    /** x 下界，对齐 chunk 边界。 */
    public static int minX(BlockPos center, int chunkRadius) { return min(center, chunkRadius, 0); }
    /** x 上界，对齐 chunk 边界。 */
    public static int maxX(BlockPos center, int chunkRadius) { return max(center, chunkRadius, 0); }
    /** y 下界，对齐 section 边界。 */
    public static int minY(BlockPos center, int chunkRadius) { return min(center, chunkRadius, 1); }
    /** y 上界，对齐 section 边界。 */
    public static int maxY(BlockPos center, int chunkRadius) { return max(center, chunkRadius, 1); }
    /** z 下界，对齐 chunk 边界。 */
    public static int minZ(BlockPos center, int chunkRadius) { return min(center, chunkRadius, 2); }
    /** z 上界，对齐 chunk 边界。 */
    public static int maxZ(BlockPos center, int chunkRadius) { return max(center, chunkRadius, 2); }

}
