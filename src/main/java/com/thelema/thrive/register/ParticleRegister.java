package com.thelema.thrive.register;

import com.mojang.serialization.MapCodec;
import com.thelema.thrive.ThriveMod;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ParticleRegister {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ThriveMod.MOD_ID);


    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> COLORED_EXPLOSION =
            PARTICLE_TYPES.register("colored_explosion", () -> new ParticleType<ColorParticleOption>(true) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });

    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> COLORED_EXPLOSION_EMITTER =
            PARTICLE_TYPES.register("colored_explosion_emitter", () -> new ParticleType<ColorParticleOption>(true) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }
                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DESERT_POPLAR_LEAVES =
            PARTICLE_TYPES.register("desert_poplar_leaves", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }

}