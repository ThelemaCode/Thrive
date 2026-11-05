package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
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
    private static final Map<ResourceKey<Level>, Set<Long>> DIRTY = new ConcurrentHashMap<>();

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
        for (Long chunkKey : touched.keySet()) markDirtyChunk(sl, chunkKey);
        for (var ce : touched.entrySet()) {
            ChunkPos cp = new ChunkPos(ce.getKey());
            ChunkAccess chunk = sl.getChunk(cp.x, cp.z, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            for (var se : ce.getValue().entrySet()) apply.apply(chunk, se.getKey(), se.getValue());
        }
        return done;
    }

    /** 改写 section 内命中 unit 的群系。 */
    public static void applyBiomeSection(ChunkAccess chunk, int sectionIndex, Set<Integer> units,
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
            int j = x - qx, k = y - qy, l = z - qz;
            if (j < 0 || j >= 4 || k < 0 || k >= 4 || l < 0 || l >= 4) return target;
            int idx = (j * 4 + k) * 4 + l;
            if (!units.contains(idx)) return cache[idx];
            return shouldReplace.test(cache[idx]) ? target : cache[idx];
        }, null, qx, qy, qz);
        chunk.setUnsaved(true);
    }

    // =======================   叶操作   =====================================

    /**
     * setBlock + 条件光照刷新。
     * <p>
     * 收 {@link LevelAccessor}：运行时（ServerLevel）改方块后按需刷新光照；
     * 生成阶段（WorldGenRegion）只 setBlock，光照由 chunk 完成时统一算。
     * <p>
     * 光照新旧值都在 {@code setBlock} 之前求值，避免依赖邻居的方块拿到错值。
     */
    public static void set(LevelAccessor level, BlockPos p, BlockState s) {
        BlockState old = level.getBlockState(p);
        if (old == s) return;

        int oldBlock = old.getLightBlock(level, p);
        int oldEmit  = old.getLightEmission(level, p);
        int newBlock = s.getLightBlock(level, p);
        int newEmit  = s.getLightEmission(level, p);

        level.setBlock(p, s, 2);

        if (level instanceof ServerLevel sl && (oldBlock != newBlock || oldEmit != newEmit))
            sl.getLightEngine().checkBlock(p);
    }

    /** 取当前维度的 dirty 集合。 */
    private static Set<Long> dirtySet(ServerLevel level) {
        return DIRTY.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());
    }

    /** 标记方块所在 chunk 待重发。只入 set，发送由 {@link #onTick} 统一做。 */
    public static void markDirty(ServerLevel level, BlockPos p) {
        dirtySet(level).add(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
    }

    /** 按 chunk key 直接标记待重发。 */
    private static void markDirtyChunk(ServerLevel level, long chunkKey) {
        dirtySet(level).add(chunkKey);
    }

    /** 每 5 tick 全局 flush 一次。 */
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 5 != 0) return;
        for (ServerLevel level : e.getServer().getAllLevels()) flush(level);
    }

    /**
     * 重发该维度所有 dirty chunk。
     * <p>
     * 未加载的 chunk 不 remove，留在 set 里等加载后再发——
     * 若直接丢弃，玩家走远再回来会看到旧光照。
     */
    public static void flush(ServerLevel level) {
        Set<Long> set = DIRTY.get(level.dimension());
        if (set == null || set.isEmpty()) return;
        var iter = set.iterator();
        while (iter.hasNext()) {
            long key = iter.next();
            ChunkPos cp = new ChunkPos(key);
            ChunkAccess chunk = level.getChunk(cp.x, cp.z, ChunkStatus.FULL, false);
            if (!(chunk instanceof LevelChunk lc)) continue;   // 未加载，下轮再试
            iter.remove();
            for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(cp, false))
                player.connection.send(new ClientboundLevelChunkWithLightPacket(lc, level.getLightEngine(), null, null));
        }
    }

    // =======================   结构替换   =====================================

    /** 从模板找 BiomeCore 相对坐标。 */
    public static BlockPos findCoreOffset(StructureTemplate template) {
        List<StructureTemplate.StructureBlockInfo> list = template.filterBlocks(
                BlockPos.ZERO, new StructurePlaceSettings(), BlockRegister.BIOME_CORE.get());
        return list.isEmpty() ? null : list.get(0).pos();
    }

    /** 用结构替换 BE，核心对齐，保留地表。 */
    public static void replaceStructure(ServerLevel level, BiomeCoreBlockEntity be, ResourceLocation toId) {
        StructureTemplate to = level.getStructureManager().get(toId).orElse(null);
        if (to == null) return;
        BlockPos corePos = be.getBlockPos();
        BlockPos toCore = findCoreOffset(to);
        if (toCore == null) return;
        BlockPos toOrigin = corePos.subtract(toCore);

        CompoundTag surfaceNbt = new CompoundTag();
        be.surface.save(surfaceNbt, level.registryAccess());
        be.surface.item = null;
        be.rotator.item = ItemStack.EMPTY;

        to.placeInWorld(level, toOrigin, toOrigin, new StructurePlaceSettings()
                        .setRotation(Rotation.NONE).setMirror(Mirror.NONE)
                        .setIgnoreEntities(false).addProcessor(BlockIgnoreProcessor.AIR),
                level.random, 50);

        if (level.getBlockEntity(corePos) instanceof BiomeCoreBlockEntity newBe) {
            newBe.surface.load(surfaceNbt, level.registryAccess());
            newBe.setChanged();
        }

        Vec3i size = to.getSize();
        for (BlockPos p : BlockPos.betweenClosed(toOrigin,
                toOrigin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
            level.getLightEngine().checkBlock(p);
            markDirty(level, p);
        }
    }

    // =======================   地理边界   =====================================

    /** 转换 y 下界，低不过世界底。 */
    public static int minY(BiomeCoreBlockEntity be) {
        int sectionMin = (be.getBlockPos().getY() >> 4) << 4;
        return Math.max(sectionMin - 32, be.getLevel().getMinBuildHeight());
    }

    /** 转换 y 上界，高不过世界顶。 */
    public static int maxY(BiomeCoreBlockEntity be) {
        int sectionMin = (be.getBlockPos().getY() >> 4) << 4;
        return Math.min(sectionMin + 47, be.getLevel().getMaxBuildHeight() - 1);
    }
}