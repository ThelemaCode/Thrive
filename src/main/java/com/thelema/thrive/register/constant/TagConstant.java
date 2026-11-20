package com.thelema.thrive.register.constant;

import com.thelema.thrive.ThriveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public interface TagConstant {

    // 物品
    TagKey<Item> HONEY = item("honey");

    // 方块
    TagKey<Block> NATURAL_SURFACE = block("natural_surface");
    TagKey<Block> NATURAL_COVER   = block("natural_cover");

    // 实体
    TagKey<EntityType<?>> CLOUD = entity("cloud");
    TagKey<EntityType<?>> IMMUNE_DEHYDRATION = entity("immune_dehydration");

    // 伤害
    TagKey<DamageType> BLUNT = damage("blunt");
    TagKey<DamageType> PIERCING = damage("piercing");
    TagKey<DamageType> SLASHING = damage("slashing");
    TagKey<DamageType> WING = damage("wind");

    // 群系
    TagKey<Biome> IS_LAND = biome("is_land");
    TagKey<Biome> IS_OCEAN = biome("is_ocean");

    // 流体
    TagKey<Fluid> CAN_SALINISE = fluid("can_salinise");

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, name));
    }

    static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, name));
    }

    private static TagKey<EntityType<?>> entity(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, name));
    }

    private static TagKey<DamageType> damage(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, name));
    }

    private static TagKey<Biome> biome(String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, name));
    }

    private static TagKey<Fluid> fluid(String name) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, name));
    }
}