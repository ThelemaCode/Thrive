package com.thelema.thrive.generic.blockentity.modifier.a.sky;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.server.level.ServerLevel;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;

/**
 * 天空扩散：BE 在 sky 群系，强制扩散，无视 200 限制。
 */
public class Sky implements BiomeModifier {

    private static final BiomeTypeChanger ANY_TO_SKY = BiomeTypeChanger.any(BiomeConstant.SKY);

    private static final Ring R_REWRITE = new Ring(10);
    private static final Ring R_CLEAR   = new Ring(10, 0, 0, 2);

    /**条件：天空*/
    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return false;
        return sl.getBiome(be.getBlockPos()).is(BiomeConstant.SKY);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean rewrite = forBS(be, R_REWRITE, ANY_TO_SKY);
        boolean clear   = forPos(be, R_CLEAR, SkyHelper::clear);
        return rewrite & clear;
    }
}