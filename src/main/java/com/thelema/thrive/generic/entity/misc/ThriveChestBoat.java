package com.thelema.thrive.generic.entity.misc;

import com.thelema.thrive.register.EntityRegister;
import com.thelema.thrive.register.ItemRegister;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** 胡杨木运输船。逻辑同 {@link ThriveBoat}，只是掉落的物品不同。 */
public class ThriveChestBoat extends ChestBoat {

    public ThriveChestBoat(EntityType<? extends ChestBoat> type, Level level) {
        super(type, level);
    }

    public ThriveChestBoat(Level level, double x, double y, double z) {
        this(EntityRegister.DESERT_POPLAR_CHEST_BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public @NotNull Item getDropItem() {
        return ItemRegister.DESERT_POPLAR_CHEST_BOAT.get();
    }
}
