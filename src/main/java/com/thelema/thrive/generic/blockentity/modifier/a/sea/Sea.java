package com.thelema.thrive.generic.blockentity.modifier.a.sea;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.server.level.ServerLevel;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;

public class Sea implements BiomeModifier {

    private static final BiomeTypeChanger WATER_TO_SEA = BiomeTypeChanger.any(BiomeConstant.SEA);
    private static final Ring R_CLEAR = new Ring(10);
    private static final Ring R_REWRITE = new Ring(10, 0, 0, 2);

    /**
     * 条件：Sea
     */
    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return false;
        return sl.getBiome(be.getBlockPos()).is(BiomeConstant.SEA);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean clear = forPos(be, R_CLEAR, SeaHelper::clear);
        boolean rewrite = forBS(be, R_REWRITE, WATER_TO_SEA);
        return clear & rewrite;
    }
}