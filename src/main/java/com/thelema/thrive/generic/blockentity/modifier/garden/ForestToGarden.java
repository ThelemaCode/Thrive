package com.thelema.thrive.generic.blockentity.modifier.garden;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.PhasedModifier;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class ForestToGarden extends PhasedModifier {

    static final ResourceKey<Biome> SOURCE = ResourceKey.create(
            Registries.BIOME, ResourceLocation.withDefaultNamespace("forest"));

    private static final BiomeTypeChanger FOREST_TO_GARDEN =
            new BiomeTypeChanger(SOURCE, GardenHelper.TARGET);

    public ForestToGarden() {
        addPos(Ring.TICK_20, ForestToGarden::clear);     // PREPARE
        addPos(Ring.TICK_20, ForestToGarden::garden);    // GARDEN
        addPos(Ring.TICK_20, ForestToGarden::boneMeal);  // BONE_MEAL
        addBS(Ring.TICK_20, FOREST_TO_GARDEN);           // REWRITE
        addFinish(GardenHelper.TREE_GARDEN);             // DONE
    }

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel().getBiome(be.getBlockPos()).is(SOURCE);
    }

    // ── 三个阶段包装：只处理 forest 地区 ──

    private static void clear(BiomeCoreBlockEntity be, BlockPos p) {
        if (!be.getLevel().getBiome(p).is(SOURCE)) return;
        GardenHelper.clear(be, p);
    }

    private static void garden(BiomeCoreBlockEntity be, BlockPos p) {
        if (!be.getLevel().getBiome(p).is(SOURCE)) return;
        GardenHelper.garden(be, p);
    }

    private static void boneMeal(BiomeCoreBlockEntity be, BlockPos p) {
        if (!be.getLevel().getBiome(p).is(SOURCE)) return;
        GardenHelper.boneMeal(be, p);
    }
}