package com.thelema.thrive.mixin;

import com.thelema.thrive.generic.tool.Damage;
import com.thelema.thrive.register.EffectRegister;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public class HealOrHarmMobEffectMixin {

    // 1. 拦截瞬间效果（喷溅/滞留药水砸中时触发）
    @Inject(method = "applyInstantenousEffect", at = @At("HEAD"), cancellable = true)
    private void thrive$interceptInstant(Entity source, Entity indirectSource, LivingEntity living, int amplifier, double health, CallbackInfo ci) {
        if (living.level().isClientSide) return;
        MobEffectInstance milk = living.getEffect(EffectRegister.MILK);
        if (milk != null){
            // 有牛奶，可以直接取消
            ci.cancel();
            // 伤害 II，12点伤害，就认为相当于 6min，7200 tick左右，一点伤害相当于 600tick
            // 原版的 伤害药水计算的公式， 6 12 24 ...
            int j = (int)(health * (double)(6 << amplifier) + (double)0.5F);

            double newDuration = milk.getDuration() - j * 600;

            if (newDuration > 0){
                // 更新
                MobEffectInstanceAccessor accessor = (MobEffectInstanceAccessor) milk;
                accessor.thrive$setDuration((int) newDuration);
                // 同步
                if (living instanceof ServerPlayer sp){
                    sp.connection.send(new ClientboundUpdateMobEffectPacket(sp.getId(), milk, false));
                }
            }else {
                // 时间小于 0，移除效果
                living.removeEffect(EffectRegister.MILK);
            }
        }
    }

    // 2. 你之前的伤害队列逻辑（如果牛奶没拦截，走到这里就进你的队列）
    @Redirect(
            method = "applyInstantenousEffect",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private boolean thrive$queueDamage(LivingEntity target, DamageSource ds, float amount) {
        if (target.level().isClientSide) return target.hurt(ds, amount);
        Damage.hurt(target, ds, amount);
        return true;
    }

    // 3. 瞬间伤害，tick调用，进队列逻辑
    @Redirect(
            method = "applyEffectTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private boolean thrive$queueTick(LivingEntity target, DamageSource ds, float amount) {
        if (target.level().isClientSide) return target.hurt(ds, amount);
        Damage.hurt(target, ds, amount);
        return true;
    }
}