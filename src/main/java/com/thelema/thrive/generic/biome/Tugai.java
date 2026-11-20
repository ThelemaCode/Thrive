package com.thelema.thrive.generic.biome;

import com.thelema.thrive.register.EffectRegister;
import com.thelema.thrive.register.constant.TagConstant;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber
public class Tugai {

    // Tugai的水，如果在水下，会导致脱水，每 10s提升一级，刷新时间为 5min
    @SubscribeEvent
    public static void dehydration(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!e.level().isClientSide && e.tickCount % 200 == 0 && e instanceof LivingEntity living && living.isAlive()
        && living.isUnderWater() && !e.getType().is(TagConstant.IMMUNE_DEHYDRATION)) {
            MobEffectInstance effect = living.getEffect(EffectRegister.DEHYDRATION);
            int i;
            if (effect != null) {
                i = effect.getDuration() + 1;
                living.removeEffect(EffectRegister.DEHYDRATION);
            }else {
                i = 0;
            }
            living.addEffect(new MobEffectInstance(
                    EffectRegister.DEHYDRATION,
                    6000,
                    Math.min(i, 4) // 最高 V，减攻速 1.0
            ));
        }
    }

}
