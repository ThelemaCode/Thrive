package com.thelema.thrive.register;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.item.RampantMoss;
import com.thelema.thrive.generic.item.Whistle;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.HoneyBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegister {

    public static final DeferredRegister<Item> ITEM = DeferredRegister.createItems(ThriveMod.MOD_ID);

    public static void register(IEventBus bus) {
        ITEM.register(bus);
    }

    private static DeferredHolder<Item, Item> simpleItem(String name) {
        return ITEM.register(name, () -> new Item(new Item.Properties()));
    }

    /**
     * 蜂蜜：食物属性与原版蜂蜜瓶完全一致。
     */
    private static DeferredHolder<Item, Item> honeyItem(String name) {
        return ITEM.register(name, () -> new HoneyBottleItem(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).food(Foods.HONEY_BOTTLE).stacksTo(16)));
    }

    // 物品
    public static final DeferredHolder<Item, Item> WHISTLE = ITEM.register("whistle", Whistle::new);
    public static final DeferredHolder<Item, Item> RAMPANT_MOSS = ITEM.register("rampant_moss", RampantMoss::new);

    // 鱼
    public static final DeferredHolder<Item, Item> MOSS_COD = ITEM.register("moss_cod", () -> new Item(new Item.Properties().food(Foods.COD)));
    public static final DeferredHolder<Item, Item> BREEZE_FISH = ITEM.register("breeze_fish", () -> new Item(new Item.Properties().food(Foods.COD)));

    // 生物蛋
    public static final DeferredHolder<Item, Item> MOSSY_ZOMBIE_SPAWN_EGG = ITEM.register("mossy_zombie_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegister.MOSSY_ZOMBIE, 0x4A5D23, 0x8B9A46, new Item.Properties()));
    public static final DeferredHolder<Item, Item> LANTERN_BERRY_CREEPER_SPAWN_EGG = ITEM.register("lantern_berry_creeper_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegister.LANTERN_BERRY_CREEPER, 0x4C8B3A, 0xFFAA22, new Item.Properties()));
    public static final DeferredHolder<Item, Item> GARDEN_BEE_SPAWN_EGG = ITEM.register("garden_bee_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegister.GARDEN_BEE, 0xFFD700, 0xFFFFFF, new Item.Properties()));
    public static final DeferredHolder<Item, Item> GARDEN_PILLAGER_SPAWN_EGG = ITEM.register("garden_pillager_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegister.GARDEN_PILLAGER, 0x5B6B47, 0xC3D96A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> GARDEN_WITCH_SPAWN_EGG = ITEM.register("garden_witch_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegister.GARDEN_WITCH, 0x3F2A56, 0x86C06C, new Item.Properties()));
    public static final DeferredHolder<Item, Item> VORTEX_BREEZE_SPAWN_EGG = ITEM.register("vortex_breeze_spawn_egg", () -> new DeferredSpawnEggItem(EntityRegister.VORTEX_BREEZE, 0x4F7A9E, 0xC8E6F0, new Item.Properties()));

    // 每种原版花对应的蜂蜜
    public static final DeferredHolder<Item, Item> DANDELION_HONEY = honeyItem("dandelion_honey");
    public static final DeferredHolder<Item, Item> POPPY_HONEY = honeyItem("poppy_honey");
    public static final DeferredHolder<Item, Item> BLUE_ORCHID_HONEY = honeyItem("blue_orchid_honey");
    public static final DeferredHolder<Item, Item> ALLIUM_HONEY = honeyItem("allium_honey");
    public static final DeferredHolder<Item, Item> AZURE_BLUET_HONEY = honeyItem("azure_bluet_honey");
    public static final DeferredHolder<Item, Item> RED_TULIP_HONEY = honeyItem("red_tulip_honey");
    public static final DeferredHolder<Item, Item> ORANGE_TULIP_HONEY = honeyItem("orange_tulip_honey");
    public static final DeferredHolder<Item, Item> WHITE_TULIP_HONEY = honeyItem("white_tulip_honey");
    public static final DeferredHolder<Item, Item> PINK_TULIP_HONEY = honeyItem("pink_tulip_honey");
    public static final DeferredHolder<Item, Item> OXEYE_DAISY_HONEY = honeyItem("oxeye_daisy_honey");
    public static final DeferredHolder<Item, Item> CORNFLOWER_HONEY = honeyItem("cornflower_honey");
    public static final DeferredHolder<Item, Item> LILY_OF_THE_VALLEY_HONEY = honeyItem("lily_of_the_valley_honey");
    public static final DeferredHolder<Item, Item> WITHER_ROSE_HONEY = honeyItem("wither_rose_honey");
    public static final DeferredHolder<Item, Item> TORCHFLOWER_HONEY = honeyItem("torchflower_honey");
    public static final DeferredHolder<Item, Item> SUNFLOWER_HONEY = honeyItem("sunflower_honey");
    public static final DeferredHolder<Item, Item> LILAC_HONEY = honeyItem("lilac_honey");
    public static final DeferredHolder<Item, Item> PEONY_HONEY = honeyItem("peony_honey");
    public static final DeferredHolder<Item, Item> ROSE_BUSH_HONEY = honeyItem("rose_bush_honey");
    public static final DeferredHolder<Item, Item> PITCHER_PLANT_HONEY = honeyItem("pitcher_plant_honey");
    public static final DeferredHolder<Item, Item> FLOWERING_AZALEA_LEAVES_HONEY = honeyItem("flowering_azalea_leaves_honey");
    public static final DeferredHolder<Item, Item> FLOWERING_AZALEA_HONEY = honeyItem("flowering_azalea_honey");
    public static final DeferredHolder<Item, Item> MANGROVE_PROPAGULE_HONEY = honeyItem("mangrove_propagule_honey");
    public static final DeferredHolder<Item, Item> CHERRY_LEAVES_HONEY = honeyItem("cherry_leaves_honey");
    public static final DeferredHolder<Item, Item> PINK_PETALS_HONEY = honeyItem("pink_petals_honey");
    public static final DeferredHolder<Item, Item> CHORUS_FLOWER_HONEY = honeyItem("chorus_flower_honey");
    public static final DeferredHolder<Item, Item> SPORE_BLOSSOM_HONEY = honeyItem("spore_blossom_honey");
}