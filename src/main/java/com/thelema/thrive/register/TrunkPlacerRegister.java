package com.thelema.thrive.register;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.worldgen.DesertPoplarTrunkPlacer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 树干生成器类型注册。
 *
 * <p>{@code trunk_placer} 是**注册表**里的类型（`minecraft:worldgen/trunk_placer_type`），
 * JSON 里写 {@code "type": "thrive:xxx"} 时，这个 key 必须在注册表里存在，
 * 否则整个注册表加载失败：
 * {@code Unknown registry key in ResourceKey[... trunk_placer_type]: thrive:xxx}。
 *
 * <p>换句话说：**写了自定义 trunk_placer 的 JSON，必须配套注册一个 TrunkPlacerType**，
 * 不能只改命名空间就完事。
 */
public class TrunkPlacerRegister {

    public static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACERS =
            DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, ThriveMod.MOD_ID);

    public static final DeferredHolder<TrunkPlacerType<?>, TrunkPlacerType<DesertPoplarTrunkPlacer>>
            DESERT_POPLAR_TRUNK_PLACER = TRUNK_PLACERS.register("desert_poplar_trunk_placer",
                    () -> new TrunkPlacerType<>(DesertPoplarTrunkPlacer.CODEC));

    public static void register(IEventBus bus) {
        TRUNK_PLACERS.register(bus);
    }
}
