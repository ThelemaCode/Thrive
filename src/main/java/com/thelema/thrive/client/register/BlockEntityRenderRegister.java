package com.thelema.thrive.client.register;

import com.thelema.thrive.client.render.blockentity.BiomeCoreBlockEntityRenderer;
import com.thelema.thrive.client.render.blockentity.SunBerryBlockEntityRenderer;
import com.thelema.thrive.register.BlockEntityRegister;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderRegister {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegister.SUN_BERRY.get(), SunBerryBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegister.BIOME_CORE.get(), BiomeCoreBlockEntityRenderer::new);

        // 告示牌的几何完全由方块实体渲染器画，方块模型是空的（只有粒子贴图）。
        // 自建的 BlockEntityType 原版不认识，所以必须手动把这两个渲染器挂上去，
        // 否则放下去只剩一圈碰撞箱边缘线。
        event.registerBlockEntityRenderer(BlockEntityRegister.THRIVE_SIGN.get(), SignRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegister.THRIVE_HANGING_SIGN.get(), HangingSignRenderer::new);
    }

}
