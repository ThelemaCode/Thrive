package com.thelema.thrive.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thelema.thrive.client.render.blockentity.cap.RotatorRenderer;
import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;

public class BiomeCoreBlockEntityRenderer implements BlockEntityRenderer<BiomeCoreBlockEntity> {

    private final ItemRenderer itemRenderer;

    public BiomeCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(BiomeCoreBlockEntity be, float partialTicks, PoseStack ms,
                       MultiBufferSource buffer, int light, int overlay) {
        RotatorRenderer.render(be.rotator, partialTicks, ms, buffer, light, overlay, itemRenderer);
    }
}