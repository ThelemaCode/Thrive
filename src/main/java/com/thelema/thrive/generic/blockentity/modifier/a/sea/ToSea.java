package com.thelema.thrive.generic.blockentity.modifier.a.sea;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;

public class ToSea implements BiomeModifier {

    private static final BiomeTypeChanger OCEAN_TO_SEA = BiomeTypeChanger.fromTag(BiomeTags.IS_OCEAN, SeaHelper.TARGET);
    private static final Ring R_CLEAR   = new Ring(10);
    private static final Ring R_REWRITE = new Ring(10, 0, 0, 2);

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return false;
        if (sl.dimension() != Level.OVERWORLD) return false;
        if (be.getBlockPos().getY() >= 200) return false;
        return sl.getBiome(be.getBlockPos()).is(BiomeTags.IS_OCEAN);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean clear   = forPos(be, R_CLEAR, SeaHelper::clear);
        boolean rewrite = forBS(be, R_REWRITE, OCEAN_TO_SEA);
        return clear & rewrite;
    }

}