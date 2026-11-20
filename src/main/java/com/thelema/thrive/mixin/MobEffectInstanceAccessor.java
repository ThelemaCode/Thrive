package com.thelema.thrive.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 直接读写 {@link MobEffectInstance} 的私有字段。
 *
 * <p>原版这个类**没有任何 setter**（`duration` / `amplifier` 都是裸 private int），
 * 想改只能 remove + new + add，那样会重新走一遍 `LivingEntity.addEffect`，
 * 很容易变成「自己触发自己」。这里把字段开出来，就能原地改。
 *
 * <p>字段都是非 final 的，所以 {@code @Accessor} 能直接生成 setter；
 * 如果是 final 字段还需要再加 {@code @Mutable}。
 */
@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {

    @Accessor("duration")
    int thrive$getDuration();

    @Accessor("duration")
    void thrive$setDuration(int duration);

    @Accessor("amplifier")
    int thrive$getAmplifier();

    @Accessor("amplifier")
    void thrive$setAmplifier(int amplifier);
}
