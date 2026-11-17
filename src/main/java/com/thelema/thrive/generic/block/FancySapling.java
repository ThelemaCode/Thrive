package com.thelema.thrive.generic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * 固定结构的树苗：一个方块对应一份 NBT，长出来就是那棵树。
 *
 * <p>锚点由注册时给死（{@code offX/offY/offZ} = 树苗那格在模板里的局部坐标），
 * 因为 {@code fancy_*_tree_*.nbt} 里没有 {@code biome_core} 标记可用。
 * 如果你愿意在 NBT 里放一个 {@code biome_core}，改用 {@link BiomeSapling} 那套按标记对齐，
 * 以后改树形就不用再改注册代码。
 */
public class FancySapling extends StructureSapling {

    private final ResourceLocation structureId;
    private final BlockPos localAnchor;

    /**
     * @param offX 树苗方块在模板内的 X（相对模板最小角）
     * @param offY 树苗方块在模板内的 Y
     * @param offZ 树苗方块在模板内的 Z
     */
    public FancySapling(String namespace, String path, int offX, int offY, int offZ) {
        this.structureId = ResourceLocation.fromNamespaceAndPath(namespace, path);
        this.localAnchor = new BlockPos(offX, offY, offZ);
    }

    @Nullable
    @Override
    protected Placement resolve(ServerLevel level, BlockPos pos, RandomSource random) {
        StructureTemplate template = level.getServer().getStructureManager().get(structureId).orElse(null);
        if (template == null) return null;
        return new Placement(template, localAnchor, Rotation.getRandom(random));
    }
}
