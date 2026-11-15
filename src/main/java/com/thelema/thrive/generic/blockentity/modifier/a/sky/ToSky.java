package com.thelema.thrive.generic.blockentity.modifier.a.sky;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.PosFinder;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;

/**
 * 天空化：主世界 y>=200 无差别转 sky，清云块。
 * 启动条件：主世界 + BE y>=200。
 */
public class ToSky implements BiomeModifier {

    private static final BiomeTypeChanger ANY_TO_SKY = BiomeTypeChanger.any(BiomeConstant.SKY);

    private static final Ring R_REWRITE = new Ring(10);
    private static final Ring R_CLEAR   = new Ring(10, 0, 0, 2);

    /**条件：主世界，200格以上*/
    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return false;
        if (sl.dimension() != Level.OVERWORLD) return false;
        return be.getBlockPos().getY() >= 200;
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean rewrite = forBS(be, ABOVE_200, ANY_TO_SKY);
        boolean clear   = forPos(be, R_CLEAR, SkyHelper::clear);
        return rewrite & clear;
    }

    /** 只让 y>=200 的坐标进 forBS。 */
    private static final PosFinder ABOVE_200 = (be, apply) ->
            R_REWRITE.find(be, (b, p) -> {
                if (p.getY() >= 200) apply.apply(b, p);
            });
}