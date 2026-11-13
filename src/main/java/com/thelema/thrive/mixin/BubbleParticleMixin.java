package com.thelema.thrive.mixin;

import com.thelema.thrive.client.MixinToolClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BubbleParticle.class)
public abstract class BubbleParticleMixin extends TextureSheetParticle {

    // 提供匹配父类的构造器，供 Mixin 编译通过
    protected BubbleParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void thrive$applyFluidColor(CallbackInfo ci) {
        // 水花生成在水面上方，向下探 0.1 格查找流体
        MixinToolClient.setColorByFluid(this, this.level, this.x, this.y, this.z, -0.1);
    }
}