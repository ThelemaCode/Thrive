package com.thelema.thrive.mixin;

import com.thelema.thrive.generic.block.LanternBerry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel; // 新增
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void thrive$useOn(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = ctx.getPlayer();
        if (player == null) return;

        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        InteractionHand hand = ctx.getHand();
        ItemStack other = player.getItemInHand(
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);

        // 规则一：目标就是 LanternBerry → 点燃，优先于任何放置
        if (state.getBlock() instanceof LanternBerry) {
            if (level instanceof ServerLevel sl) {
                // 传入 ServerLevel
                LanternBerry.ignite(sl, pos, player);
                // 播放音效
                sl.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                        1.0F, sl.random.nextFloat() * 0.4F + 0.8F);
                // 消耗耐久
                ctx.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            // 双端都需要返回 SUCCESS，避免客户端预测失败导致重复点击
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        // 规则二：另一手拿浆果 → 让位给副手放置，不点火
        if (other.getItem() instanceof BlockItem bi && bi.getBlock() instanceof LanternBerry) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}