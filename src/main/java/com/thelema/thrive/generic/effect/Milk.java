package com.thelema.thrive.generic.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

/**
 * 牛奶：一个纯标记效果。
 *
 * <p>全部逻辑在 {@code LivingEntityMixin#thrive$milkGuard}（拦截 {@code addEffect}）和
 * {@code HealOrHarmMobEffectMixin}（瞬间伤害/治疗），这里只在效果本身上留两件事：
 * <ul>
 *   <li>{@code NEUTRAL} 分类：不算负面效果，也不会被自己的盾挡掉；</li>
 *   <li>{@code fillEffectCures} 留空：牛奶不能被牛奶清除，否则扣时长时会把盾自己清掉。</li>
 * </ul>
 */
public class Milk extends MobEffect {

    public Milk() {
        super(MobEffectCategory.NEUTRAL, 0xFFFFFFFF);
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        // 故意留空：牛奶自身不可被牛奶治愈
    }
}
