package com.thelema.thrive.register;

import com.thelema.thrive.ThriveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber
public class PotionRegister {

    static final DeferredRegister<Potion> POTION =
            DeferredRegister.create(Registries.POTION, ThriveMod.MOD_ID);

    // ============ 缓降 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_SLOW_FALLING =
            POTION.register("super_long_slow_falling", () -> new Potion("slow_falling",
                    new MobEffectInstance(MobEffects.SLOW_FALLING, 19200)));

    // ============ 牛奶 ============
    public static final DeferredHolder<Potion, Potion> MILK =
            POTION.register("milk", () -> new Potion("milk",
                    new MobEffectInstance(EffectRegister.MILK, 3600))); // 3min
    public static final DeferredHolder<Potion, Potion> LONG_MILK =
            POTION.register("long_milk", () -> new Potion("milk",
                    new MobEffectInstance(EffectRegister.MILK, 9600))); // 8 min
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_MILK =
            POTION.register("super_long_milk", () -> new Potion("milk",
                    new MobEffectInstance(EffectRegister.MILK, 38400)));// 32min

    // ============ 迅捷 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_SWIFTNESS =
            POTION.register("super_long_swiftness", () -> new Potion("swiftness",
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 38400)));
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_SWIFTNESS =
            POTION.register("super_strong_swiftness", () -> new Potion("swiftness",
                    new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 2)));

    // ============ 力量 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_STRENGTH =
            POTION.register("super_long_strength", () -> new Potion("strength",
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, 38400)));
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_STRENGTH =
            POTION.register("super_strong_strength", () -> new Potion("strength",
                    new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1800, 2)));

    // ============ 抗火 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_FIRE_RESISTANCE =
            POTION.register("super_long_fire_resistance", () -> new Potion("fire_resistance",
                    new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 38400)));

    // ============ 跳跃 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_LEAPING =
            POTION.register("super_long_leaping", () -> new Potion("leaping",
                    new MobEffectInstance(MobEffects.JUMP, 38400)));
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_LEAPING =
            POTION.register("super_strong_leaping", () -> new Potion("leaping",
                    new MobEffectInstance(MobEffects.JUMP, 1800, 2)));

    // ============ 恢复 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_REGENERATION =
            POTION.register("super_long_regeneration", () -> new Potion("regeneration",
                    new MobEffectInstance(MobEffects.REGENERATION, 7200)));
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_REGENERATION =
            POTION.register("super_strong_regeneration", () -> new Potion("regeneration",
                    new MobEffectInstance(MobEffects.REGENERATION, 450, 2)));

    // ============ 隐身 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_INVISIBILITY =
            POTION.register("super_long_invisibility", () -> new Potion("invisibility",
                    new MobEffectInstance(MobEffects.INVISIBILITY, 38400)));

    // ============ 虚弱 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_WEAKNESS =
            POTION.register("super_long_weakness", () -> new Potion("weakness",
                    new MobEffectInstance(MobEffects.WEAKNESS, 19200)));

    // ============ 水肺 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_WATER_BREATHING =
            POTION.register("super_long_water_breathing", () -> new Potion("water_breathing",
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 38400)));

    // ============ 剧毒 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_POISON =
            POTION.register("super_long_poison", () -> new Potion("poison",
                    new MobEffectInstance(MobEffects.POISON, 7200)));
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_POISON =
            POTION.register("super_strong_poison", () -> new Potion("poison",
                    new MobEffectInstance(MobEffects.POISON, 432, 2)));

    // ============ 缓慢 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_SLOWNESS =
            POTION.register("super_long_slowness", () -> new Potion("slowness",
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 19200)));
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_SLOWNESS =
            POTION.register("super_strong_slowness", () -> new Potion("slowness",
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 3)));

    // ============ 夜视 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_NIGHT_VISION =
            POTION.register("super_long_night_vision", () -> new Potion("night_vision",
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 38400)));

    // ============ 织网 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_WEAVING =
            POTION.register("super_long_weaving", () -> new Potion("weaving",
                    new MobEffectInstance(MobEffects.WEAVING, 14400)));

    // ============ 渗浆 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_OOZING =
            POTION.register("super_long_oozing", () -> new Potion("oozing",
                    new MobEffectInstance(MobEffects.OOZING, 14400)));

    // ============ 虫蚀 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_INFESTED =
            POTION.register("super_long_infested", () -> new Potion("infested",
                    new MobEffectInstance(MobEffects.INFESTED, 14400)));

    // ============ 蓄风 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_WIND_CHARGED =
            POTION.register("super_long_wind_charged", () -> new Potion("wind_charged",
                    new MobEffectInstance(MobEffects.WIND_CHARGED, 14400)));

    // ============ 凋零 ============
    public static final DeferredHolder<Potion, Potion> WITHER =
            POTION.register("wither", () -> new Potion("wither",
                    new MobEffectInstance(MobEffects.WITHER, 900))); // 45s
    public static final DeferredHolder<Potion, Potion> LONG_WITHER =
            POTION.register("long_wither", () -> new Potion("wither",
                    new MobEffectInstance(MobEffects.WITHER, 1800))); // 90s
    public static final DeferredHolder<Potion, Potion> STRONG_WITHER =
            POTION.register("strong_wither", () -> new Potion("wither",
                    new MobEffectInstance(MobEffects.WITHER, 450, 1))); // 22s II
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_WITHER =
            POTION.register("super_long_wither", () -> new Potion("wither", // 360s 6min
                    new MobEffectInstance(MobEffects.WITHER, 7200)));
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_WITHER =
            POTION.register("super_strong_wither", () -> new Potion("wither",
                    new MobEffectInstance(MobEffects.WITHER, 450, 2))); // 22s III

    // ============ 治疗（瞬发） ============
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_HEALING =
            POTION.register("super_strong_healing", () -> new Potion("healing",
                    new MobEffectInstance(MobEffects.HEAL, 1, 2)));

    // ============ 伤害（瞬发） ============
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_HARMING =
            POTION.register("super_strong_harming", () -> new Potion("harming",
                    new MobEffectInstance(MobEffects.HARM, 1, 2)));

    // ============ 神龟 ============
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_TURTLE_MASTER =
            POTION.register("super_long_turtle_master", () -> new Potion("turtle_master",
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3200, 3),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3200, 2)));
    public static final DeferredHolder<Potion, Potion> SUPER_STRONG_TURTLE_MASTER =
            POTION.register("super_strong_turtle_master", () -> new Potion("turtle_master",
                    new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 3),
                    new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 4)));

    // ============ 漂浮 ============
    public static final DeferredHolder<Potion, Potion> LEVITATION =
            POTION.register("levitation", () -> new Potion("levitation",
                    new MobEffectInstance(MobEffects.LEVITATION, 1800)));
    public static final DeferredHolder<Potion, Potion> LONG_LEVITATION =
            POTION.register("long_levitation", () -> new Potion("levitation",
                    new MobEffectInstance(MobEffects.LEVITATION, 4800)));
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_LEVITATION =
            POTION.register("super_long_levitation", () -> new Potion("levitation",
                    new MobEffectInstance(MobEffects.LEVITATION, 19200)));

    // ============ 苔藓 ============
    public static final DeferredHolder<Potion, Potion> MOSS =
            POTION.register("moss", () -> new Potion("moss",
                    new MobEffectInstance(EffectRegister.MOSS, 900))); // 45s
    public static final DeferredHolder<Potion, Potion> LONG_MOSS =
            POTION.register("long_moss", () -> new Potion("moss",
                    new MobEffectInstance(EffectRegister.MOSS, 1800))); // 90s
    public static final DeferredHolder<Potion, Potion> SUPER_LONG_MOSS =
            POTION.register("super_long_moss", () -> new Potion("moss",
                    new MobEffectInstance(EffectRegister.MOSS, 7200))); // 360s


    public static void register(IEventBus bus) {
        POTION.register(bus);
    }

    // ============ 配方 ============

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder b = event.getBuilder();

        // 白郁金香 → 牛奶
        b.addMix(Potions.AWKWARD, Items.WHITE_TULIP, MILK);
        b.addMix(MILK, Items.REDSTONE, LONG_MILK);

        // 凋灵玫瑰 → 凋零
        b.addMix(Potions.AWKWARD, Items.WITHER_ROSE, WITHER);
        b.addMix(WITHER, Items.REDSTONE, LONG_WITHER);
        b.addMix(WITHER, Items.GLOWSTONE_DUST, STRONG_WITHER);

        // 紫颂花 → 漂浮
        b.addMix(Potions.AWKWARD, Items.CHORUS_FLOWER, LEVITATION);
        b.addMix(LEVITATION, Items.REDSTONE, LONG_LEVITATION);

        // 苔藓鳕鱼 → 苔藓
        b.addMix(Potions.AWKWARD, ItemRegister.MOSS_COD.get(), MOSS);
        b.addMix(MOSS, Items.REDSTONE, LONG_MOSS);

        // 蜂蜜升级配方
        honeyRecipe(b);
    }

    private static void honeyRecipe(PotionBrewing.Builder b) {
        // 1 蒲公英 → 缓降
        b.addMix(Potions.LONG_SLOW_FALLING, ItemRegister.DANDELION_HONEY.get(), SUPER_LONG_SLOW_FALLING);

        // 2 虞美人 → 治疗
        b.addMix(Potions.STRONG_HEALING, ItemRegister.POPPY_HONEY.get(), SUPER_STRONG_HEALING);

        // 3 兰花 → 水肺
        b.addMix(Potions.LONG_WATER_BREATHING, ItemRegister.BLUE_ORCHID_HONEY.get(), SUPER_LONG_WATER_BREATHING);

        // 4 绒球葱 → 织网
        b.addMix(Potions.WEAVING, ItemRegister.ALLIUM_HONEY.get(), SUPER_LONG_WEAVING);

        // 5 蓝花美耳草 → 隐身
        b.addMix(Potions.LONG_INVISIBILITY, ItemRegister.AZURE_BLUET_HONEY.get(), SUPER_LONG_INVISIBILITY);

        // 6 红郁金香 → 力量
        b.addMix(Potions.LONG_STRENGTH, ItemRegister.RED_TULIP_HONEY.get(), SUPER_LONG_STRENGTH);
        b.addMix(Potions.STRONG_STRENGTH, ItemRegister.RED_TULIP_HONEY.get(), SUPER_STRONG_STRENGTH);

        // 7 橙郁金香 → 抗火
        b.addMix(Potions.LONG_FIRE_RESISTANCE, ItemRegister.ORANGE_TULIP_HONEY.get(), SUPER_LONG_FIRE_RESISTANCE);

        // 8 白郁金香 → 牛奶
        b.addMix(PotionRegister.LONG_MILK, ItemRegister.WHITE_TULIP_HONEY.get(), SUPER_LONG_MILK);

        // 9 粉郁金香 → 恢复
        b.addMix(Potions.LONG_REGENERATION, ItemRegister.PINK_TULIP_HONEY.get(), SUPER_LONG_REGENERATION);
        b.addMix(Potions.STRONG_REGENERATION, ItemRegister.PINK_TULIP_HONEY.get(), SUPER_STRONG_REGENERATION);

        // 10 滨菊 → 虚弱
        b.addMix(Potions.LONG_WEAKNESS, ItemRegister.OXEYE_DAISY_HONEY.get(), SUPER_LONG_WEAKNESS);

        // 11 矢车菊 → 迅捷
        b.addMix(Potions.LONG_SWIFTNESS, ItemRegister.CORNFLOWER_HONEY.get(), SUPER_LONG_SWIFTNESS);
        b.addMix(Potions.STRONG_SWIFTNESS, ItemRegister.CORNFLOWER_HONEY.get(), SUPER_STRONG_SWIFTNESS);

        // 12 铃兰 → 剧毒
        b.addMix(Potions.LONG_POISON, ItemRegister.LILY_OF_THE_VALLEY_HONEY.get(), SUPER_LONG_POISON);
        b.addMix(Potions.STRONG_POISON, ItemRegister.LILY_OF_THE_VALLEY_HONEY.get(), SUPER_STRONG_POISON);

        // 13 凋灵玫瑰 → 凋零
        b.addMix(LONG_WITHER,   ItemRegister.WITHER_ROSE_HONEY.get(), SUPER_LONG_WITHER);
        b.addMix(STRONG_WITHER, ItemRegister.WITHER_ROSE_HONEY.get(), SUPER_STRONG_WITHER);

        // 16 丁香 → 蓄风
        b.addMix(Potions.WIND_CHARGED, ItemRegister.LILAC_HONEY.get(), SUPER_LONG_WIND_CHARGED);

        // 18 玫瑰丛 → 伤害
        b.addMix(Potions.STRONG_HARMING, ItemRegister.ROSE_BUSH_HONEY.get(), SUPER_STRONG_HARMING);

        // 19 瓶子草 → 渗浆
        b.addMix(Potions.OOZING, ItemRegister.PITCHER_PLANT_HONEY.get(), SUPER_LONG_OOZING);

        // 20 盛开杜鹃叶 → 虫蚀
        b.addMix(Potions.INFESTED, ItemRegister.FLOWERING_AZALEA_LEAVES_HONEY.get(), SUPER_LONG_INFESTED);

        // 21 盛开杜鹃 → 跳跃，苔藓
        b.addMix(Potions.LONG_LEAPING, ItemRegister.FLOWERING_AZALEA_HONEY.get(), SUPER_LONG_LEAPING);
        b.addMix(Potions.STRONG_LEAPING, ItemRegister.FLOWERING_AZALEA_HONEY.get(), SUPER_STRONG_LEAPING);
        b.addMix(LONG_MOSS, ItemRegister.FLOWERING_AZALEA_HONEY.get(), SUPER_LONG_MOSS);

        // 22 红树胎生苗 → 神龟
        b.addMix(Potions.LONG_TURTLE_MASTER, ItemRegister.MANGROVE_PROPAGULE_HONEY.get(), SUPER_LONG_TURTLE_MASTER);
        b.addMix(Potions.STRONG_TURTLE_MASTER, ItemRegister.MANGROVE_PROPAGULE_HONEY.get(), SUPER_STRONG_TURTLE_MASTER);

        // 24 粉红花瓣 → 缓慢
        b.addMix(Potions.LONG_SLOWNESS, ItemRegister.PINK_PETALS_HONEY.get(), SUPER_LONG_SLOWNESS);
        b.addMix(Potions.STRONG_SLOWNESS, ItemRegister.PINK_PETALS_HONEY.get(), SUPER_STRONG_SLOWNESS);

        // 25 紫颂花 → 漂浮
        b.addMix(LONG_LEVITATION, ItemRegister.CHORUS_FLOWER_HONEY.get(), SUPER_LONG_LEVITATION);

        // 26 孢子花 → 夜视
        b.addMix(Potions.LONG_NIGHT_VISION, ItemRegister.SPORE_BLOSSOM_HONEY.get(), SUPER_LONG_NIGHT_VISION);

    }

}