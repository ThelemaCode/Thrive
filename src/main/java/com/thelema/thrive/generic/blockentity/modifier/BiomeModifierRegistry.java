package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.blockentity.modifier.cloud.SkyToCloud;
import com.thelema.thrive.generic.blockentity.modifier.garden.ForestToGarden;
import com.thelema.thrive.generic.blockentity.modifier.garden.Garden;
import com.thelema.thrive.generic.blockentity.modifier.moss.Moss;
import com.thelema.thrive.generic.blockentity.modifier.moss.PlainsToMoss;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.*;

public class BiomeModifierRegistry {

    public static final Map<ResourceLocation, BiomeModifier> BY_ID = new HashMap<>();

    /**
     * 物品 ID 列表（已排序） -> 候选修改器 id 列表
     */
    private static final Map<List<ResourceLocation>, List<ResourceLocation>> ITEM_LIST_TO_ID_LIST = new HashMap<>();

    public static void register(String name, List<Item> items, BiomeModifier modifier) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID, name);
        BY_ID.put(id, modifier);

        // 转换为 ResourceLocation 并利用其自带的 Comparable 排序
        List<ResourceLocation> key = items.stream()
                .map(BuiltInRegistries.ITEM::getKey)
                .sorted()
                .toList();

        ITEM_LIST_TO_ID_LIST.computeIfAbsent(key, k -> new ArrayList<>()).add(id);
    }

    public static BiomeModifier get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static List<ResourceLocation> getCandidates(List<Item> items) {
        List<ResourceLocation> key = items.stream()
                .map(BuiltInRegistries.ITEM::getKey)
                .sorted()
                .toList();

        List<ResourceLocation> list = ITEM_LIST_TO_ID_LIST.get(key);
        return list == null ? List.of() : Collections.unmodifiableList(list);
    }

    static {
        register("plains_to_moss", List.of(Items.MOSS_BLOCK), new PlainsToMoss());
        register("moss", List.of(Items.MOSS_BLOCK), new Moss());
        register("forest_to_garden", List.of(Items.HONEY_BLOCK), new ForestToGarden());
        register("garden", List.of(Items.HONEY_BLOCK), new Garden());
        register("cloud", List.of(BlockRegister.CLOUD.get().asItem()), new SkyToCloud());
    }
}