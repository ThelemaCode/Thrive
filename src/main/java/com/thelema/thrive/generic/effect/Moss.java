package com.thelema.thrive.generic.effect;

import com.thelema.thrive.register.EffectRegister;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;


public class Moss extends MobEffect {


    public Moss() {
        super(MobEffectCategory.BENEFICIAL, 0x4A7C3F);
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 着火时清掉再生，和 MossyZombie.tickRegen 一致
        if (entity.getRemainingFireTicks() > 0) {
            entity.removeEffect(MobEffects.REGENERATION);
            return true;
        }
        // 有浆果汁时提升到 VIII 级
        int amp = entity.hasEffect(EffectRegister.LANTERN_BERRY_JUICE) ? 7 : 3;
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, amp, true, true));
        return true;
    }

}