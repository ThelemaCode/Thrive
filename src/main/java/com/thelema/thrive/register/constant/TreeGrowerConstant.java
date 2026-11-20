package com.thelema.thrive.register.constant;

import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public interface TreeGrowerConstant {

    TreeGrower DESERT_POPLAR = new TreeGrower("desert_poplar", Optional.empty(), Optional.of(ConfiguredFeatureConstant.DESERT_POPLAR_FEATURE), Optional.empty());
}
