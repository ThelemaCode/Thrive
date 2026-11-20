package com.thelema.thrive.generic.effect;

import com.thelema.thrive.ThriveMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

public class Dehydration extends MobEffect {

    public Dehydration() {
        super(MobEffectCategory.HARMFUL, 0xFF4B0082);
        // 每级，降低 0.2 攻速
        addAttributeModifier(
                Attributes.ATTACK_SPEED,
                ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, "effect.dehydration"),
                -0.2,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 200 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        // V 级别时，附带虚弱 II，周期性失明
        if (amplifier >= 4){
            living.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS, 200, 2
            ));
            living.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS, 100
            ));
        }
        return super.applyEffectTick(living, amplifier);
    }
}
