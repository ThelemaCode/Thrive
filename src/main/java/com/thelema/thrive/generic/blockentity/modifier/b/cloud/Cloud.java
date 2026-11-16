package com.thelema.thrive.generic.blockentity.modifier.b.cloud;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * 云扩散器：BE 已在 cloud 群系，向外继续撒云，完成后替换结构。
 */
public class Cloud implements BiomeModifier {

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel sl)) return false;
        BlockPos p = be.getBlockPos();
        return sl.getChunk(p).getNoiseBiome(p.getX() >> 2, p.getY() >> 2, p.getZ() >> 2)
                .is(BiomeConstant.CLOUD);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        return SkyToCloud.clouds(be);
    }
}