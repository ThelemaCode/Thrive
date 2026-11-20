package com.thelema.thrive.register.constant;

import com.thelema.thrive.ThriveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public interface ConfiguredFeatureConstant {

    ResourceKey<ConfiguredFeature<?, ?>> DESERT_POPLAR_FEATURE = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, "desert_poplar"));

}
