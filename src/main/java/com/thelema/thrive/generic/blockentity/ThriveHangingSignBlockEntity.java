package com.thelema.thrive.generic.blockentity;

import com.thelema.thrive.register.BlockEntityRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 自己的悬挂告示牌方块实体。理由同 {@link ThriveSignBlockEntity}。
 *
 * <p>{@code HangingSignBlockEntity} 没有接收 type 的构造器（内部固定成
 * {@code BlockEntityType.HANGING_SIGN}），所以只能覆写 {@link #getType()}。
 */
public class ThriveHangingSignBlockEntity extends HangingSignBlockEntity {

    public ThriveHangingSignBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public @NotNull BlockEntityType<?> getType() {
        return BlockEntityRegister.THRIVE_HANGING_SIGN.get();
    }
}
