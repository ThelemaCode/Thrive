package com.thelema.thrive.mixin;

import com.thelema.thrive.client.MixinToolClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashParticle.class)
public abstract class SplashParticleMixin extends TextureSheetParticle {

    protected SplashParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void thrive$applyFluidColor(CallbackInfo ci) {
        // 水花生成在水面上方，向下探 0.1 格查找流体
        MixinToolClient.setColorByFluid(this, this.level, this.x, this.y, this.z, -0.1);
    }
}