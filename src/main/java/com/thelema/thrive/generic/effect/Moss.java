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

    private static final int INTERVAL = 20;
    private static final int REGEN_DURATION = 40;
    private static final int AMP = 3;        // IV
    private static final int AMP_BUFFED = 7; // VIII

    public Moss() {
        super(MobEffectCategory.BENEFICIAL, 0x4A7C3F);
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 着火时清掉再生，和 MossyZombie.tickRegen 一致
        if (entity.getRemainingFireTicks() > 0) {
            entity.removeEffect(MobEffects.REGENERATION);
            return true;
        }
        // 有浆果汁时提升到 VIII 级
        int amp = entity.hasEffect(EffectRegister.LANTERN_BERRY_JUICE) ? AMP_BUFFED : AMP;
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION, amp, true, true));
        return true;
    }

}