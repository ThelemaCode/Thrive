package com.thelema.thrive;

import com.thelema.thrive.register.FluidRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class MixinTool {

    public static boolean isThriveFluid(FluidState fluidState) {
        if (fluidState.isEmpty()) return false;
        ResourceLocation id = NeoForgeRegistries.FLUID_TYPES.getKey(fluidState.getFluidType());
        return id != null && id.getNamespace().equals(ThriveMod.MOD_ID);
    }

    public static boolean isThriveBlock(BlockState blockState) {
        if (blockState.isAir()) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        return id.getNamespace().equals(ThriveMod.MOD_ID);
    }

    public static boolean isInThriveFluidType(Entity e){
        return e.isInFluidType(FluidRegister.SKY_WATER_TYPE.get())
                || e.isInFluidType(FluidRegister.LANTERN_BERRY_JUICE_TYPE.get());
    }

}
