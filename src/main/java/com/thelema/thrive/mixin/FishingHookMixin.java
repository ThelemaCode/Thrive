package com.thelema.thrive.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.thelema.thrive.MixinTool;
import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.entity.misc.BaitArea;
import com.thelema.thrive.generic.entity.mob.VortexBreeze;
import com.thelema.thrive.generic.fishing.FishingRuleLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;


@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    // 1 & 2: 拦截 tick 里的水判定，让鱼钩在任意本模组流体里能成功浮起
    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean thrive$allowCustomFluidInTick(boolean original, @Local FluidState fluidstate) {
        return original || MixinTool.isThriveFluid(fluidstate);
    }

    // 3: 拦截 catchingFish 里的液体方块判定（允许生成粒子和咬钩）
    @ModifyExpressionValue(
            method = "catchingFish",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z")
    )
    private boolean thrive$allowCustomFluidParticles(boolean original, @Local BlockState blockstate) {
        return original || MixinTool.isThriveBlock(blockstate);
    }

    // 4: 拦截开阔水域判定
    @ModifyExpressionValue(
            method = "getOpenWaterTypeForBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean thrive$allowCustomFluidOpenWater(boolean original, @Local FluidState fluidstate) {
        return original || MixinTool.isThriveFluid(fluidstate);
    }

    // 5: 拦截战利品表获取
    @ModifyExpressionValue(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ReloadableServerRegistries$Holder;getLootTable(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/storage/loot/LootTable;")
    )
    private LootTable thrive$changeJuiceLootTable(LootTable original) {
        FishingHook hook = (FishingHook) (Object) this;
        if (!(hook.level() instanceof ServerLevel serverLevel)) return original;

        // 1. 获取当前环境 ID 字符串
        FluidState fs = serverLevel.getFluidState(hook.blockPosition());
        if (fs.isEmpty()) return original;

        ResourceLocation fluidId = NeoForgeRegistries.FLUID_TYPES.getKey(fs.getFluidType());
        if (fluidId == null) return original;
        String f = fluidId.toString();
        if (f.endsWith("_type")) {
            f = f.substring(0, f.length() - 5);
        }

        ResourceLocation biomeId = serverLevel.getBiome(hook.blockPosition()).unwrapKey()
                .map(ResourceKey::location).orElse(null);
        String b = biomeId != null ? biomeId.toString() : "any";

        // 获取鱼饵
        AABB area = AABB.ofSize(hook.position(), 13.0, 13.0, 13.0);
        BaitArea nearestBaitArea = serverLevel.getEntitiesOfClass(BaitArea.class, area)
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(hook)))
                .orElse(null);

        String ba = (nearestBaitArea != null && nearestBaitArea.baitItemId != null)
                ? nearestBaitArea.baitItemId.toString() : null;

        // 2. 遍历规则并寻找最优解
        JsonObject bestMatch = null;
        int highestScore = -1;

        for (JsonObject rule : FishingRuleLoader.RULES) {
            int score = 0;
            boolean match = true;

            // 检查流体
            if (rule.has("fluid")) {
                String ruleFluid = rule.get("fluid").getAsString();
                if (!ruleFluid.equals("any")) {
                    if (!ruleFluid.equals(f)) { match = false; }
                    else { score++; }
                }
            }

            // 检查群系
            if (match && rule.has("biome")) {
                String ruleBiome = rule.get("biome").getAsString();
                if (!ruleBiome.equals("any")) {
                    if (!ruleBiome.equals(b)) { match = false; }
                    else { score++; }
                }
            }

            // 检查鱼饵
            if (match && rule.has("bait")) {
                String ruleBait = rule.get("bait").getAsString();
                if (!ruleBait.equals("any")) {
                    if (!ruleBait.equals(ba)) { match = false; }
                    else { score++; }
                }
            }

            // 如果匹配成功，且条件比上一个更严苛（分数更高），则替换
            if (match && score > highestScore) {
                highestScore = score;
                bestMatch = rule;
            }
        }

        // 3. 如果找到了匹配的规则，直接加载战利品表
        if (bestMatch != null && bestMatch.has("loot_table")) {
            ResourceLocation tableId = ResourceLocation.parse(bestMatch.get("loot_table").getAsString());
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, tableId);
            LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(key);
            if (table != LootTable.EMPTY) {
                ThriveMod.LOGGER.info("钓鱼查表 -> 流体: {}, 鱼饵: {}", f, ba);
                return table;
            }
        }


        // 4. 未命中任何自定义表，返回原版
        return original;
    }

    // 6. 增大对实体的拉动
    @Redirect(
            method = "pullEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
    )
    private void thrive$enhancePullForce(Entity entity, Vec3 originalDelta) {
        FishingHook hook = (FishingHook) (Object) this;
        Entity owner = hook.getOwner();

        if (entity instanceof VortexBreeze && owner != null) {
            // 原版是 0.1D，增加两倍变成 0.3D
            Vec3 vec3 = new Vec3(
                    owner.getX() - hook.getX(),
                    owner.getY() - hook.getY(),
                    owner.getZ() - hook.getZ()
            ).scale(0.3D);
            entity.setDeltaMovement(entity.getDeltaMovement().add(vec3));
            return;
        }
        // 其他实体走原版逻辑
        entity.setDeltaMovement(originalDelta);
    }


}