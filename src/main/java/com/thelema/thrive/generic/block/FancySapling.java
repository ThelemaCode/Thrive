package com.thelema.thrive.generic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;

public class FancySapling extends SaplingBlock {

    private final ResourceLocation structureId;
    private final Vec3i offset;

    /**
     * @param structureId 结构ID
     * @param offX 树苗相对于结构X最小值的偏移
     * @param offY 树苗相对于结构Y最小值的偏移
     * @param offZ 树苗相对于结构Z最小值的偏移
     */
    public FancySapling(String namespace, String path, int offX, int offY, int offZ) {
        // 假 TreeGrower，完全由 advanceTree 接管
        super(new TreeGrower("dummy", Optional.empty(), Optional.empty(), Optional.empty()),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PLANT)
                        .noCollission()
                        .randomTicks()
                        .instabreak()
                        .sound(SoundType.GRASS));
        this.structureId = ResourceLocation.fromNamespaceAndPath(namespace, path);
        this.offset = new Vec3i(offX, offY, offZ);
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        // 第一阶段，先变大
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
            return;
        }

        StructureTemplateManager manager = level.getServer().getStructureManager();
        StructureTemplate template = manager.get(structureId).orElse(null);
        if (template == null) return;

        // 【核心计算】用树苗位置减去偏移，反推出结构最小角 (0,0,0) 应该放置的位置
        BlockPos toOrigin = pos.subtract(offset);

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false)
                .addProcessor(BlockIgnoreProcessor.AIR);

        // 移除树苗本身
        level.removeBlock(pos, false);

        // 生成结构
        template.placeInWorld(level, toOrigin, toOrigin, settings, level.random,
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);

        // 更新光照
        Vec3i size = template.getSize();
        BlockPos toEnd = toOrigin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        for (BlockPos p : BlockPos.betweenClosed(toOrigin, toEnd)) {
            level.getLightEngine().checkBlock(p);
        }
    }
}