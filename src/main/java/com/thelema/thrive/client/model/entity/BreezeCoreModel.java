package com.thelema.thrive.client.model.entity;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.entity.mob.VortexBreeze;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BreezeCoreModel extends GeoModel<VortexBreeze> {

    @Override
    public ResourceLocation getModelResource(VortexBreeze vortexBreeze) {
        return ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, "geo/converge_breeze.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VortexBreeze vortexBreeze) {
        return ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, "textures/entity/converge_breeze/converge_breeze.png");
    }

    @Override
    public ResourceLocation getAnimationResource(VortexBreeze vortexBreeze) {
        return ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, "animations/converge_breeze.animation.json");

    }
}