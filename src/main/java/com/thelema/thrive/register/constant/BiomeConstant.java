package com.thelema.thrive.register.constant;

import com.thelema.thrive.ThriveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public interface BiomeConstant {



    // 原始星球形态
    ResourceKey<Biome> ROCK = biome("rock");
    ResourceKey<Biome> SKY = biome("sky");
    ResourceKey<Biome> SEA = biome("sea");

    // I
    ResourceKey<Biome> GARDEN = biome("garden");
    ResourceKey<Biome> MOSS = biome("moss");
    ResourceKey<Biome> CLOUD = biome("cloud");
    ResourceKey<Biome> TUGAI = biome("tugai");

    // II
    ResourceKey<Biome> SUNBERRY = biome("sunberry");

    private static ResourceKey<Biome> biome(String name){
        return ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, name));
    }
}
