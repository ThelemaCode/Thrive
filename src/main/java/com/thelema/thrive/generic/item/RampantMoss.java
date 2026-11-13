package com.thelema.thrive.generic.item;

import com.thelema.thrive.generic.entity.misc.BaitArea;
import com.thelema.thrive.register.EntityRegister;
import com.thelema.thrive.register.FluidRegister;
import com.thelema.thrive.register.ItemRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
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
            if (level instanceof ServerLevel serverLevel) {
                spawnBaitArea(serverLevel, pos, player);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }

    private void spawnBaitArea(ServerLevel level, BlockPos pos, Player player) {
        // 使用你更新后的实体注册名
        BaitArea area = new BaitArea(EntityRegister.BAIT_AREA.get(), level);
        area.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.0F, 0.0F);

        // 【核心更新】设置鱼饵的 ID，取代原来的 lootTable
        // 这里假设该物品打窝放出的诱饵是“苔藓鳕鱼”，如果不是请替换为对应的物品
        area.baitItemId = BuiltInRegistries.ITEM.getKey(ItemRegister.RAMPANT_MOSS.get());

        // 或者直接使用当前手持物品的 ID：area.baitItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        level.addFreshEntity(area);
        level.playSound(null, pos, SoundEvents.MOSS_CARPET_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
    }
}