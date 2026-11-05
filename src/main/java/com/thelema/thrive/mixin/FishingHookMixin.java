package com.thelema.thrive.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.thelema.thrive.generic.entity.misc.FishingArea;
import com.thelema.thrive.register.FluidRegister;
import com.thelema.thrive.register.ParticleRegister;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Comparator;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    // 1 & 2: 拦截 tick 里的水判定，让鱼钩在果汁里能成功浮起
    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean thrive$allowJuiceInTick(boolean original, @Local FluidState fluidstate) {
        return original || fluidstate.getFluidType() == FluidRegister.LANTERN_BERRY_JUICE_TYPE.get();
    }

    // 3: 拦截 catchingFish 里的液体方块判定（允许生成粒子和咬钩）
    @ModifyExpressionValue(
            method = "catchingFish",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private boolean thrive$allowJuiceParticles(boolean original, @Local BlockState blockstate) {
        return original || blockstate.is(FluidRegister.LANTERN_BERRY_JUICE_BLOCK.get());
    }

    // 4: 拦截开阔水域判定
    @ModifyExpressionValue(
            method = "getOpenWaterTypeForBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean thrive$allowJuiceOpenWater(boolean original, @Local FluidState fluidstate) {
        return original || fluidstate.getFluidType() == FluidRegister.LANTERN_BERRY_JUICE_TYPE.get();
    }

    // 5: 拦截战利品表获取
    @ModifyExpressionValue(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ReloadableServerRegistries$Holder;getLootTable(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/storage/loot/LootTable;")
    )
    private LootTable thrive$changeJuiceLootTable(LootTable original) {
        FishingHook hook = (FishingHook) (Object) this;
        ServerLevel serverLevel = (ServerLevel) hook.level();

        // 1. 优先找 13x13x13 范围内最近的 FishingArea
        AABB area = AABB.ofSize(hook.position(), 13.0, 13.0, 13.0);
        FishingArea nearestArea = serverLevel.getEntitiesOfClass(FishingArea.class, area)
                .stream()
                .min(Comparator.comparingDouble(a -> a.distanceToSqr(hook)))
                .orElse(null);

        if (nearestArea != null && nearestArea.lootTable != null) {
            return serverLevel.getServer().reloadableRegistries().getLootTable(nearestArea.lootTable);
        }

        // 2. 其次根据流体类型动态获取
        FluidState fluidState = serverLevel.getFluidState(hook.blockPosition());

        if (!fluidState.isEmpty() && fluidState.getFluidType() != NeoForgeMod.WATER_TYPE.value()) {
            ResourceLocation fluidId = NeoForgeRegistries.FLUID_TYPES.getKey(fluidState.getFluidType());

            if (fluidId != null) {
                ResourceKey<LootTable> customLootTable = ResourceKey.create(
                        Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath(fluidId.getNamespace(), "fishing/" + fluidId.getPath())
                );
                return serverLevel.getServer().reloadableRegistries().getLootTable(customLootTable);
            }
        }

        // 3. 原版默认
        return original;
    }

    // 6: 替换果汁里的粒子（水花、气泡、汇聚水流）
    @ModifyArg(
            method = "catchingFish",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"),
            index = 0
    )
    private ParticleOptions thrive$changeJuiceParticles(ParticleOptions original) {
        FishingHook hook = (FishingHook) (Object) this;
        if (hook.level().getFluidState(hook.blockPosition()).getFluidType() == FluidRegister.LANTERN_BERRY_JUICE_TYPE.get()) {
            // 原版水花和汇聚水流 -> 换成我们的彩色水花（带物理碰撞）
            if (original == ParticleTypes.SPLASH || original == ParticleTypes.FISHING) {
                return ParticleRegister.JUICE_SPLASH.get();
            }
            // 原版气泡 -> 换成我们绑定了新贴图的气泡类
            if (original == ParticleTypes.BUBBLE) {
                return ParticleRegister.JUICE_BUBBLE.get();
            }
        }
        return original;
    }
}