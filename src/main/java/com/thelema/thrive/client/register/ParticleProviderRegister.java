package com.thelema.thrive.client.register;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.client.particle.ColoredExplosionParticle;
import com.thelema.thrive.client.particle.DesertPoplarLeafParticle;
import com.thelema.thrive.client.particle.ExplosionEmitterParticle;
import com.thelema.thrive.register.ParticleRegister;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = ThriveMod.MOD_ID, value = Dist.CLIENT)
public class ParticleProviderRegister {

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        // 染色爆炸粒子
        event.registerSpriteSet(ParticleRegister.COLORED_EXPLOSION.get(), ColoredExplosionParticle.Provider::new);

        // 燃爆爆炸粒子投射粒子
        event.registerSpecial(ParticleRegister.COLORED_EXPLOSION_EMITTER.get(), new ExplosionEmitterParticle.Provider(ParticleRegister.COLORED_EXPLOSION.get()));

        // 胡杨落叶粒子
        event.registerSpriteSet(ParticleRegister.DESERT_POPLAR_LEAVES.get(), DesertPoplarLeafParticle.Provider::new);

    }
}