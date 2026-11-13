package com.thelema.thrive.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.thelema.thrive.register.FluidRegister;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin{


    // 精准拦截 travel 方法里的 isInWater() 调用
    @ModifyExpressionValue(
            method = "travel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z")
    )
    private boolean thrive$fakeWaterInTravel(boolean original) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 如果实体接触到了你的流体
        if (self.isInFluidType(FluidRegister.SKY_WATER_TYPE.get()) ||
                self.isInFluidType(FluidRegister.LANTERN_BERRY_JUICE_TYPE.get())) {
            return true; // 强行告诉 travel：我在水里！
        }
        return original;
    }
}