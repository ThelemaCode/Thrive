package com.thelema.thrive.generic.blockentity.modifier.b.tugai;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.server.level.ServerLevel;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.skip;

/**
 * 胡杨林扩散器：BE 已经在 TUGAI 群系里，把胡杨林继续往外推。
 *
 * <p>和 {@link DesertToTugai}（转换器）的分工：
 * <ul>
 *   <li>转换器：BE 在别的群系，先把整个立方体改成 Tugai；</li>
 *   <li>扩散器：BE 已经在 Tugai，只处理**还没被同化**的那部分（{@code BiomeUtil.skip(TUGAI, ...)}），
 *       用来从已有林区边缘继续吞掉旁边的沙漠。</li>
 * </ul>
 *
 * <p>几层都用同一个 {@link Ring}，靠 delay 错开：地表 → 水池 → 树 → 草 → 改群系。
 * 后面每一步看到的都是前一步已经铺好的地面，所以顺序不能换。
 */
public class Tugai implements BiomeModifier {

    private static final BiomeTypeChanger ANY_TO_TUGAI = BiomeTypeChanger.any(BiomeConstant.TUGAI);

    /** 每一层落后上一层几环。 */
    private static final int DELAY = 2;

    private static final Ring R_SURFACE = new Ring(20, 0, 0, 0);
    private static final Ring R_POOL = new Ring(20, 0, 0, DELAY);
    private static final Ring R_TREE = new Ring(20, 0, 0, DELAY * 2);
    private static final Ring R_GRASS = new Ring(20, 0, 0, DELAY * 3);
    private static final Ring R_REWRITE = new Ring(20, 0, 0, DELAY * 4);

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel() instanceof ServerLevel sl && sl.getBiome(be.getBlockPos()).is(BiomeConstant.TUGAI);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean surface = forPos(be, R_SURFACE, skip(BiomeConstant.TUGAI, TugaiHelper::surface));
        boolean pool = forPos(be, R_POOL, skip(BiomeConstant.TUGAI, TugaiHelper::pool));
        boolean tree = forPos(be, R_TREE, skip(BiomeConstant.TUGAI, TugaiHelper::tree));
        boolean grass = forPos(be, R_GRASS, skip(BiomeConstant.TUGAI, TugaiHelper::grass));

        // 群系最后改：前面几步都在"还不是 Tugai"的地盘上作业
        boolean rewrite = forBS(be, R_REWRITE, ANY_TO_TUGAI);

        return surface & pool & tree & grass & rewrite;
    }
}
