package com.thelema.thrive.generic.blockentity.modifier.b.garden;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import net.minecraft.server.level.ServerLevel;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.*;

/**
 * 森林 → 花园（转化器，完成时替换结构）。
 */
public class ForestToGarden implements BiomeModifier {

    private static final BiomeTypeChanger FOREST_TO_GARDEN =
            BiomeTypeChanger.of(GardenHelper.FOREST_SOURCE, GardenHelper.TARGET);

    private static final int DELAY = 2;

    private static final Ring R_CLEAR     = new Ring(20, 0, 0, 0);
    private static final Ring R_GARDEN    = new Ring(20, 0, 0, DELAY);
    private static final Ring R_BONE_MEAL = new Ring(20, 0, 0, DELAY * 2);
    private static final Ring R_REWRITE   = new Ring(20, 0, 0, DELAY * 3);

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel().getBiome(be.getBlockPos()).is(GardenHelper.FOREST_SOURCE);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean clear    = forPos(be, R_CLEAR,     GardenHelper.onlyForest(GardenHelper::clear));
        boolean garden   = forPos(be, R_GARDEN,    GardenHelper.onlyForest(GardenHelper::garden));
        boolean boneMeal = forPos(be, R_BONE_MEAL, GardenHelper.onlyForest(GardenHelper::boneMeal));
        boolean rewrite  = forBS (be, R_REWRITE,   FOREST_TO_GARDEN);

        boolean all = clear & garden & boneMeal & rewrite;
        if (all) placeTree((ServerLevel) be.getLevel(), be, GardenHelper.TREE_GARDEN);
        return all;
    }
}