package com.thelema.thrive.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.thelema.thrive.register.EffectRegister;
import com.thelema.thrive.register.FluidRegister;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {


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

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void thrive$milkGuard(MobEffectInstance instance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        // 客户端不管
        if (self.level().isClientSide) return;

        if (instance.getEffect().is(EffectRegister.MILK.getKey())) {
            // 新增的是牛奶

            // 自己没有牛奶 buff，进入初次清理逻辑
            if (!self.hasEffect(EffectRegister.MILK)) {
                // 计算，牛奶能够添加的时间
                int need = 0;
                for (MobEffectInstance effect : self.getActiveEffects()) {
                    if (effect.getCures().contains(EffectCures.MILK)){
                        need += effect.getDuration();
                    }
                }
                // need 太多， milk无法成功添加，仅仅是消耗掉 buff而已
                if (need >= instance.getDuration()) {
                    // 添加 milk buff 失败，时间不够
                    cir.setReturnValue(false);
                }else {
                    // 更新时间
                    MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) instance;
                    accessor.thrive$setDuration(instance.getDuration() - need);
                    // 同步
                    if (self instanceof ServerPlayer sp){
                        sp.connection.send(new ClientboundUpdateMobEffectPacket(sp.getId(), instance, false));
                    }
                    // 放行，后续自然修改
                }
            }

            // 无论如何，清除全部 牛奶可解的 buff
            self.removeEffectsCuredBy(EffectCures.MILK);

            // 放行

        } else if (instance.getCures().contains(EffectCures.MILK)) {
            // 可被牛奶治愈
            MobEffectInstance milk = self.getEffect(EffectRegister.MILK);
            if (milk != null) {

                // 存在牛奶，直接返回，false，不让添加
                cir.setReturnValue(false);


                // 消耗 持续时间，这里都是非瞬间，不需要额外计算
                int newDuration = milk.getDuration() - instance.getDuration();
                // 新时间 > 0
                if (newDuration > 0) {
                    // 更新 buff
                    MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) milk;
                    accessor.thrive$setDuration(newDuration);
                    // 同步
                    if (self instanceof ServerPlayer sp){
                        sp.connection.send(new ClientboundUpdateMobEffectPacket(sp.getId(), milk, false));
                    }
                }else {
                    self.removeEffect(EffectRegister.MILK);
                }

            }

        }

    }
}
