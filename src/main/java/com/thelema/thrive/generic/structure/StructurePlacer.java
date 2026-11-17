package com.thelema.thrive.generic.structure;

import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 「把一份 NBT 模板摆到世界上」的唯一入口。
 *
 * <p>以前这套逻辑在 {@code FancySapling}、{@code BiomeSapling}、{@code BiomeUtil.placeTree}
 * 里各写了一遍，而且三处的对齐方式还不一样（手算偏移 / rotationPivot / 直接减），
 * 这是「逻辑混乱」的根源。这里统一成一份，调用方只需要回答两个问题：
 * <ol>
 *   <li>模板里**哪一块**是锚点（{@code localAnchor}，模板内的局部坐标）；</li>
 *   <li>这棵树的**树苗/核心**落在世界哪个位置（{@code anchor}）。</li>
 * </ol>
 *
 * <p>对齐用的技巧：把 {@code rotationPivot} 设为 {@code localAnchor}，
 * 于是锚点在**任意旋转下都是不动点**，{@code origin = anchor - localAnchor} 恒成立。
 * 手算 {@code transform(offset)} 的那套写法（原 {@code FancySapling.getToOrigin}）
 * 数学上等价，但没有必要。
 */
public final class StructurePlacer {

    /**
     * 与 jigsaw 世界生成同款：只同步客户端 + 不触发形状连锁。
     * 树里全是原木/树叶/栅栏这类不需要「连接」的方块，用 18 比 UPDATE_ALL 快一个数量级。
     * 如果你的模板里有需要和外面连起来的方块（栅栏、墙、红石），改成 {@link Block#UPDATE_ALL}。
     */
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private StructurePlacer() {
    }

    /**
     * 按锚点对齐放置一份模板。
     *
     * @param anchor      世界里锚点（树苗 / 核心）的坐标
     * @param localAnchor 模板内锚点的坐标（模板局部坐标，从最小角算起）
     * @return 和 {@code placeInWorld} 一致：false 只代表「一个方块都没写」，不代表全部成功
     */
    public static boolean place(ServerLevel level, StructureTemplate template,
                                BlockPos anchor, BlockPos localAnchor,
                                Rotation rotation, RandomSource random) {
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                // 锚点当旋转轴心：锚点不动，于是 origin = anchor - localAnchor 永远成立
                .setRotationPivot(localAnchor)
                .setIgnoreEntities(false)
                // 模板里的空气不落地，否则模板里的空腔会把世界挖出洞
                .addProcessor(BlockIgnoreProcessor.AIR)
                // 只覆盖空气/花草/水/树叶，别的一律跳过（解决「过分替换」）
                .addProcessor(OnlyReplaceableProcessor.INSTANCE);

        BlockPos origin = anchor.subtract(localAnchor);
        return template.placeInWorld(level, origin, anchor, settings, random, FLAGS);
    }

    /**
     * 在模板里找第一块标记方块（默认是 {@code BIOME_CORE}）的局部坐标。
     *
     * @return 找不到返回 {@code null}，调用方应当放弃这次放置
     */
    @Nullable
    public static BlockPos findMarker(StructureTemplate template, Block block) {
        List<StructureTemplate.StructureBlockInfo> found =
                template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block);
        return found.isEmpty() ? null : found.get(0).pos();
    }

    /** {@code BIOME_CORE} 标记的局部坐标，找不到返回 {@code null}。 */
    @Nullable
    public static BlockPos findCoreMarker(StructureTemplate template) {
        return findMarker(template, BlockRegister.BIOME_CORE.get());
    }
}
