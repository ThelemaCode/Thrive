package com.thelema.thrive.generic.blockentity.modifier.a.rock;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.server.level.ServerLevel;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;

public class Rock implements BiomeModifier {

    private static final BiomeTypeChanger ANY_TO_ROCK = BiomeTypeChanger.any(BiomeConstant.ROCK);
    private static final Ring R_TERRAIN = new Ring(10);
    private static final Ring R_REWRITE = new Ring(10, 0, 0, 2);

    /**条件：Rock*/
    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return false;
        return sl.getBiome(be.getBlockPos()).is(BiomeConstant.ROCK);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean terrain = forPos(be, R_TERRAIN, RockHelper.skipRock(RockHelper::terrain));
        boolean rewrite = forBS(be, R_REWRITE, ANY_TO_ROCK);
        return terrain & rewrite;
    }
}