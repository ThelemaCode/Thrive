package com.thelema.thrive.generic.blockentity.modifier.a.rock;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.constant.BiomeConstant;
import com.thelema.thrive.register.constant.TagConstant;
import net.minecraft.server.level.ServerLevel;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;

public class ToRock implements BiomeModifier {

    private static final BiomeTypeChanger LAND_TO_ROCK = BiomeTypeChanger.fromTag(TagConstant.IS_LAND, BiomeConstant.ROCK);
    private static final Ring R_REWRITE = new Ring(10);
    private static final Ring R_TERRAIN = new Ring(10, 0, 0, 2);

    /**条件：低于200格，陆地群系标签 */
    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return false;
        if (be.getBlockPos().getY() >= 200) return false;
        return sl.getBiome(be.getBlockPos()).is(TagConstant.IS_LAND);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean rewrite = forBS(be, R_REWRITE, LAND_TO_ROCK);
        boolean terrain = forPos(be, R_TERRAIN, RockHelper::terrain);   // 处理所有坐标
        return rewrite & terrain;
    }
}