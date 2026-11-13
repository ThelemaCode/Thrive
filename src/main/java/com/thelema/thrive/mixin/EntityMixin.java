package com.thelema.thrive.mixin;

import com.thelema.thrive.register.FluidRegister;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
    private void thrive$windChargedDeflection(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        // 1. 穿透箭不受蓄风II影响
        if (projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            return;
        }

        Entity self = (Entity) (Object) this;

        // 2. 处理蓄风 II 效果
        if (self instanceof LivingEntity living) {
            MobEffectInstance effect = living.getEffect(MobEffects.WIND_CHARGED);

            if (effect != null && effect.getAmplifier() >= 1) {
                cir.setReturnValue((proj, entity, random) -> {
                    if (entity != null) entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BREEZE_DEFLECT, entity.getSoundSource(), 1.0F, 1.0F);
                    ProjectileDeflection.REVERSE.deflect(proj, entity, random);
                });
            }
        }
    }

    // 我们借用 NeoForge 的 persistentData 来记录上一 tick 的状态，避免加新字段
    @Unique
    private boolean thrive$wasInCustomFluid = false;

    @Shadow
    protected boolean firstTick;

    @Inject(method = "updateInWaterStateAndDoWaterCurrentPushing", at = @At("RETURN"))
    private void thrive$triggerCustomSplash(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;

        // 1. 获取当前是否在自定义流体中
        FluidState fs = self.level().getFluidState(self.blockPosition());
        boolean inCustomFluid = fs.getFluidType() == FluidRegister.SKY_WATER_TYPE.get()
                || fs.getFluidType() == FluidRegister.LANTERN_BERRY_JUICE_TYPE.get();

        // 2. 如果这一 tick 进去了，但上一 tick 没进去 -> 刚刚入水！
        if (inCustomFluid && !thrive$wasInCustomFluid && !firstTick) {
            ((EntityAccessor) self).invokeDoWaterSplashEffect();
            // 重置摔落伤害（原版在水里也会重置）
            self.resetFallDistance();
        }

        // 3. 更新记录
        thrive$wasInCustomFluid = inCustomFluid;
    }

}