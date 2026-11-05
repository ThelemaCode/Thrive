package com.thelema.thrive.generic.item;

import com.thelema.thrive.generic.entity.misc.FishingArea;
import com.thelema.thrive.register.EntityRegister;
import com.thelema.thrive.register.FluidRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class RampantMoss extends Item {

    public RampantMoss() {
        super(new Properties());
    }

    // 针对深水区/直接点水面的情况
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 使用 SOURCE_ONLY 让射线能直接打在流体源头上
        BlockHitResult hitResult = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);

        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos pos = hitResult.getBlockPos();
        FluidState fs = level.getFluidState(pos);

        // 判定是否是果汁流体
        if (fs.getFluidType() == FluidRegister.LANTERN_BERRY_JUICE_TYPE.get()) {
            if (!level.isClientSide) {
                spawnFishingArea(level, pos, player);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }

    private void spawnFishingArea(Level level, BlockPos pos, Player player) {
        FishingArea area = new FishingArea(EntityRegister.FISHING_AREA.get(), level);
        area.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.0F, 0.0F);

        area.lootTable = ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath("thrive", "fishing/lantern_berry_juice"));

        level.addFreshEntity(area);
        level.playSound(null, pos, SoundEvents.MOSS_CARPET_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
    }
}