package com.thelema.thrive.client.register;

import com.thelema.thrive.client.model.entity.GardenBeeModel;
import com.thelema.thrive.client.render.entity.*;
import com.thelema.thrive.register.EntityRegister;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber
@OnlyIn(Dist.CLIENT)
public class EntityRenderRegister {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(EntityRegister.MOSSY_ZOMBIE.get(), MossyZombieRenderer::new);
        e.registerEntityRenderer(EntityRegister.PRIMED_LANTERN_BERRY.get(), PrimedLanternBerryRenderer::new);
        e.registerEntityRenderer(EntityRegister.LANTERN_BERRY_CREEPER.get(), LanternBerryCreeperRenderer::new);
        e.registerEntityRenderer(EntityRegister.GARDEN_BEE.get(), GardenBeeRenderer::new);
        e.registerEntityRenderer(EntityRegister.PRIMED_SUN_BERRY.get(), PrimedSunBerryRenderer::new);
        e.registerEntityRenderer(EntityRegister.GARDEN_WITCH.get(), GardenWitchRenderer::new);
        e.registerEntityRenderer(EntityRegister.GARDEN_PILLAGER.get(), GardenPillagerRenderer::new);
        e.registerEntityRenderer(EntityRegister.SUN.get(), SunRenderer::new);
        e.registerEntityRenderer(EntityRegister.VORTEX_BREEZE.get(), BreezeCoreRenderer::new);
        e.registerEntityRenderer(EntityRegister.BAIT_AREA.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GardenBeeModel.LAYER_LOCATION, GardenBeeModel::createBodyLayer);
    }

}
