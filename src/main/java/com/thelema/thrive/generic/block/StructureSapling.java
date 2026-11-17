package com.thelema.thrive.generic.block;

import com.thelema.thrive.generic.structure.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 「用 NBT 模板长树」的树苗基类。
 *
 * <p>原版树苗靠 {@code TreeGrower → TreeFeature} 随机生成，长什么样控制不了；
 * 想要美术上精确的形状就只能自己摆结构（NBT 模板）。
 * 代价是原版那套「只往空气里放、放不下就换地方」的保护全都没有了，
 * 所以放置必须走 {@link StructurePlacer}（它带「只覆盖可替换方块」的处理器）。
 *
 * <p>子类只需要回答「长哪棵树、锚点在哪」。
 */
public abstract class StructureSapling extends SaplingBlock {

    /**
     * 假 grower：{@link #advanceTree} 被完整重写，原版 TreeGrower 一次都不会被调用。
     * 三个 Optional 全空是为了让原版随机树彻底不触发。
     */
    private static final TreeGrower NO_GROWER =
            new TreeGrower("thrive_structure_sapling", Optional.empty(), Optional.empty(), Optional.empty());

    protected StructureSapling() {
        super(NO_GROWER, BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.GRASS));
    }

    /**
     * 决定这一次长哪棵树。
     *
     * @return {@code null} 表示放弃（模板缺失 / 模板里没有锚点标记），树苗原地保留
     */
    @Nullable
    protected abstract Placement resolve(ServerLevel level, BlockPos pos, RandomSource random);

    /** 一次放置的完整描述：哪份模板、模板里哪一格对准树苗、转到哪个角度。 */
    protected record Placement(StructureTemplate template, BlockPos localAnchor, Rotation rotation) {
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        // 第一阶段先「长大」：骨粉/随机刻第一次只推进 stage，第二次才真正成树（同原版）
        // flags 4 与原版 SaplingBlock.advanceTree 一致，客户端同步走 chunk 的 changedBlocks 通道
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
            return;
        }

        Placement placement = this.resolve(level, pos, random);
        if (placement == null) return;

        // 先删树苗：树苗本身是「可替换」的，留着会挡在模板的树苗格里
        level.removeBlock(pos, false);

        StructurePlacer.place(level, placement.template(), pos, placement.localAnchor(),
                placement.rotation(), random);
    }
}
