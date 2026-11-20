package com.thelema.thrive.generic.entity.misc;

import com.thelema.thrive.register.EntityRegister;
import com.thelema.thrive.register.ItemRegister;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * 胡杨木船。
 *
 * <p>没有自定义 {@code Boat.Type}：本项目只有一种木头，变体字段纯属多余，
 * 模型和贴图由 {@code ThriveBoatRenderer} 直接写死。
 * 这也避开了「拓展原版枚举」那套 {@code enumextensions} 机制。
 */
public class ThriveBoat extends Boat {

    public ThriveBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    public ThriveBoat(Level level, double x, double y, double z) {
        this(EntityRegister.DESERT_POPLAR_BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public @NotNull Item getDropItem() {
        return ItemRegister.DESERT_POPLAR_BOAT.get();
    }
}
