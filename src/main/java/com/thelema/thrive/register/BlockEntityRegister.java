package com.thelema.thrive.register;


import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.blockentity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockEntityRegister {

    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThriveMod.MOD_ID);

    public static void register(IEventBus bus) {
        BLOCK_ENTITY.register(bus);
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BiomeCoreBlockEntity>> BIOME_CORE;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SunBerryBlockEntity>> SUN_BERRY;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GardenBeeNestBlockEntity>> GARDEN_BEE_NEST;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThriveSignBlockEntity>> THRIVE_SIGN;
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThriveHangingSignBlockEntity>> THRIVE_HANGING_SIGN;

    static {

        THRIVE_SIGN = BLOCK_ENTITY.register("thrive_sign", () -> BlockEntityType.Builder
                .of(ThriveSignBlockEntity::new,
                        BlockRegister.DESERT_POPLAR_SIGN.get(),
                        BlockRegister.DESERT_POPLAR_WALL_SIGN.get())
                .build(null));

        THRIVE_HANGING_SIGN = BLOCK_ENTITY.register("thrive_hanging_sign", () -> BlockEntityType.Builder
                .of(ThriveHangingSignBlockEntity::new,
                        BlockRegister.DESERT_POPLAR_HANGING_SIGN.get(),
                        BlockRegister.DESERT_POPLAR_WALL_HANGING_SIGN.get())
                .build(null));

        SUN_BERRY = BLOCK_ENTITY.register("sun_berry", () -> BlockEntityType.Builder
                .of(SunBerryBlockEntity::new, BlockRegister.SUN_BERRY.get())
                .build(null));

        BIOME_CORE = BLOCK_ENTITY.register("biome_core", () -> BlockEntityType.Builder
                .of(BiomeCoreBlockEntity::new, BlockRegister.BIOME_CORE.get())
                .build(null));

        GARDEN_BEE_NEST = BLOCK_ENTITY.register("garden_bee_nest", () -> BlockEntityType.Builder
                .of(GardenBeeNestBlockEntity::new, BlockRegister.GARDEN_BEE_NEST.get())
                .build(null));

    }


}

