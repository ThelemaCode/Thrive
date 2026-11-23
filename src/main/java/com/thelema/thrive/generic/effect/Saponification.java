package com.thelema.thrive.generic.effect;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.register.EffectRegister;
import com.thelema.thrive.register.constant.TagConstant;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Set;

/**
 * 皂化反应：碱与体内脂质反应，破坏组织。
 */
@EventBusSubscriber
public class Saponification extends MobEffect {

    /**每级降低 1 点最大生命值（{@code amount} 是每级值，框架会乘 {@code amplifier + 1}）。*/
    public Saponification() {
        super(MobEffectCategory.HARMFUL, 0xFF4B0082);
        addAttributeModifier(
                Attributes.MAX_HEALTH,
                ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, "effect.saponification"),
                -1.0F,
                AttributeModifier.Operation.ADD_VALUE
        );
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        // 牛奶能治愈            体内的药水性，魔法性外来物：原版中毒（视为药水性中毒，无实际物质，魔法性的中毒）凋零（生命凋谢，魔法性）
        // 牛奶不能治愈          体表状态，诅咒，体内有害物质，组织病变，生命活动所需物质的缺乏。（体表附着的浆果汁，与碱反应的身体组织，脱水导致的无力）
    }

    /**
     * 提高撕裂类伤害，每级提升 10% 伤害。
     * */
    @SubscribeEvent
    public static void boostSlashingDamage(LivingIncomingDamageEvent event) {
        LivingEntity living = event.getEntity();
        MobEffectInstance effect = living.getEffect(EffectRegister.SAPONIFICATION);
        // 具有 buff
        if (effect != null) {
            int a = effect.getAmplifier();
            DamageContainer container = event.getContainer();
            Holder<DamageType> holder = container.getSource().typeHolder();
            // 属于撕裂
            if (holder.is(TagConstant.SLASHING)) {
                container.setNewDamage(container.getNewDamage() * (1 + 0.1f * a));
            }
        }

    }

    /**
     * 创面在遇到水会加重
     * */
    @SubscribeEvent
    public static void worsensWithWater(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living && living.tickCount % 100 == 0 && living.isInWaterRainOrBubble()) {
            worsen(living);
        }
    }

    /**恶化，如果连 I 级都没有，返回false*/
    public static boolean worsen(LivingEntity living) {
        MobEffectInstance effect = living.getEffect(EffectRegister.SAPONIFICATION);
        if (effect != null) {
            living.addEffect(new MobEffectInstance(EffectRegister.SAPONIFICATION, effect.getDuration(), effect.getAmplifier() + 1));
            return true;
        }
        return false;
    }
}
