package com.thelema.thrive.client.render.entity;

import com.thelema.thrive.client.model.entity.BreezeCoreModel;
import com.thelema.thrive.client.render.entity.misc.BreezeCoreWindLayer;
import com.thelema.thrive.generic.entity.mob.VortexBreeze;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BreezeCoreRenderer extends GeoEntityRenderer<VortexBreeze> {

    public BreezeCoreRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BreezeCoreModel());
        addRenderLayer(new BreezeCoreWindLayer(this));
    }


}
