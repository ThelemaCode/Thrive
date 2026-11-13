package com.thelema.thrive.mixin;

import com.thelema.thrive.client.MixinToolClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.WakeParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WakeParticle.class)
public abstract class WakeParticleMixin extends TextureSheetParticle {

    protected WakeParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void thrive$applyFluidColor(CallbackInfo ci) {
        // 牵引粒子就在水面，yOffset 传 0 即可
        MixinToolClient.setColorByFluid(this, this.level, this.x, this.y, this.z, -0.1);
    }
}