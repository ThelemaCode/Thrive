package com.thelema.thrive.generic.effect;

import com.thelema.thrive.ThriveMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
}
