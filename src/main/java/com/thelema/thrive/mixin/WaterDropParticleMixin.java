package com.thelema.thrive.mixin;

import com.thelema.thrive.client.MixinToolClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.WaterDropParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WaterDropParticle.class)
public abstract class WaterDropParticleMixin extends TextureSheetParticle {

    protected WaterDropParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void thrive$applyFluidColor(CallbackInfo ci) {
        // 雨滴粒子生成在地面上，传 0 即可
        MixinToolClient.setColorByFluid(this, this.level, this.x, this.y, this.z, 0);
    }
}