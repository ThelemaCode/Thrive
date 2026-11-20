package com.thelema.thrive.generic.blockentity;

import com.thelema.thrive.register.BlockEntityRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 自己的告示牌方块实体。
 *
 * <p>原版 {@code SignBlockEntity} 的 type 写死在 {@code BlockEntityType.SIGN} 上，
 * 而 {@code BlockEntityType} 的合法方块集合是构造时固定的、之后不能追加，
 * 所以模组告示牌必须自带一个 {@code BlockEntityType}，再用这个类把 type 指过去。
 */
public class ThriveSignBlockEntity extends SignBlockEntity {

    public ThriveSignBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegister.THRIVE_SIGN.get(), pos, state);
    }

    @Override
    public @NotNull BlockEntityType<?> getType() {
        return BlockEntityRegister.THRIVE_SIGN.get();
    }
}
