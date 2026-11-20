package com.thelema.thrive.generic.item;

import com.thelema.thrive.generic.entity.misc.DiffusionZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

public class DiffusibleItem extends Item {
    private final int lifetime;
    @Nullable private final TagKey<Fluid> fluidTag;   // null = 不限制
    @Nullable ParticleOptions particle;

    public DiffusibleItem(Properties props, int lifetime, @Nullable TagKey<Fluid> fluidTag, @Nullable ParticleOptions particle) {
        super(props);
        this.lifetime = lifetime;
        this.fluidTag = fluidTag;
        this.particle = particle;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 使用 SOURCE_ONLY 让射线能直接打在流体源头上
        BlockHitResult hitResult = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (hitResult.getType() != HitResult.Type.MISS) {
            BlockPos pos = hitResult.getBlockPos();

            // 只能洒在特定流体中
            FluidState fs = level.getFluidState(pos);
            if (fluidTag != null && !fs.is(fluidTag)) {
                return InteractionResultHolder.pass(stack);
            }

            if (level instanceof ServerLevel sl) {
                // 不能堆叠
                List<DiffusionZone> ofClass = sl.getEntitiesOfClass(DiffusionZone.class, new AABB(pos).inflate(1.0f));
                for (DiffusionZone zone : ofClass) {
                    if (zone.blockPosition().equals(pos)){
                        return InteractionResultHolder.pass(stack);
                    }
                }

                // 生成 扩散区，物品：猖獗生长的苔藓
                DiffusionZone.spawnInWorld(sl, pos, BuiltInRegistries.ITEM.getKey(stack.getItem()), lifetime, particle);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.success(stack);
            }
        }


        return InteractionResultHolder.pass(stack);
    }
}
