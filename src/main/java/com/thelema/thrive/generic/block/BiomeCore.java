package com.thelema.thrive.generic.block;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.register.BlockEntityRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BiomeCore extends Block implements EntityBlock {

    public BiomeCore() {
        super(Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(5.0f)
                .sound(SoundType.STONE)
                .noOcclusion());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BiomeCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != BlockEntityRegister.BIOME_CORE.get()) return null;
        return (lvl, pos, st, be) -> BiomeCoreBlockEntity.tick(lvl, pos, st, (BiomeCoreBlockEntity) be);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof BiomeCoreBlockEntity be) {
            be.resetConversion();
            be.rotator.dropAll();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BiomeCoreBlockEntity be))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // 空手放行，交给 useWithoutItem
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            // 机器转化中
            if (be.activeBiomeModifierId != null) {
                if (player.isShiftKeyDown()) {
                    terminate(level, pos, be, player); // 潜行打断
                    return ItemInteractionResult.SUCCESS;
                }
                return ItemInteractionResult.FAIL; // 不潜行，拒绝放入
            }

            // 机器空闲，正常放入
            be.insertItem(player, stack);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM,
                    SoundSource.BLOCKS, 0.75F, 1.0F);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BiomeCoreBlockEntity be))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            // 机器转化中
            if (be.activeBiomeModifierId != null) {
                if (player.isShiftKeyDown()) {
                    terminate(level, pos, be, player); // 潜行打断
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS; // 不潜行，无反应
            }

            // 机器空闲，正常取出
            if (player.isShiftKeyDown()) {
                // 潜行：一次性全拿下来
                while (!be.rotator.isEmpty()) {
                    be.extractItem(player);
                }
            } else {
                // 普通空手：拿下来一个
                be.extractItem(player);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    SoundSource.BLOCKS, 0.75F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void terminate(Level level, BlockPos pos, BiomeCoreBlockEntity be, Player player) {
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.getX() + 0.5, pos.getY() + be.rotator.oy, pos.getZ() + 0.5,
                    30, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 1.0F, 1.0F);

            Vec3 dir = player.position().subtract(Vec3.atCenterOf(pos)).normalize();
            player.push(dir.x * 1.5, 0.4, dir.z * 1.5);
            player.hurtMarked = true;

            // 【修改】不掉落物品，直接重置机器（resetConversion 内部会清空 Rotator）
            be.resetConversion();
        }
    }
}