package com.thelema.thrive.generic.effect;

import com.thelema.thrive.ThriveMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AlkalineBurn extends MobEffect {

    public AlkalineBurn() {
        super(MobEffectCategory.HARMFUL, 0xFF4B0082);
        // 每级，降低 1 点最大生命值
        addAttributeModifier(
                Attributes.MAX_HEALTH,
                ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, "effect.alkaline_burn"),
                -1.0F,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

}