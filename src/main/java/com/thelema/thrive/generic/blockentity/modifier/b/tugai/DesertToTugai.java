package com.thelema.thrive.generic.blockentity.modifier.b.tugai;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.PhasedModifier;
import com.thelema.thrive.generic.blockentity.modifier.Ray;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.only;

/**
 * 荒漠 → 胡杨林（Tugai）。
 *
 * <p>一个 phase 就是一个"必须整体跑完才能进入下一步"的阶段，顺序不能乱：
 * <ol>
 *   <li><b>群系</b>：先整片改成 TUGAI，后面所有阶段都靠 {@code only(TUGAI)} 过滤；</li>
 *   <li><b>地表</b>：沙 → 尘土，砂岩 → 方解石/盐块；</li>
 *   <li><b>水池</b>：必须在树之前，否则树会长在水里；</li>
 *   <li><b>胡杨</b>：此时地表还是干净的，{@code p.above()} 是空气；</li>
 *   <li><b>黄草</b>：必须最后，否则草会挡住树的落脚点。</li>
 * </ol>
 *
 * <p>前两阶段用 {@link Ring}（必须扫过每一列），后三阶段用 {@link Ray}（只要地表那一格，便宜得多）。
 */
public class DesertToTugai extends PhasedModifier {

    static final BiomeTypeChanger CHANGER = BiomeTypeChanger.of(Biomes.DESERT, BiomeConstant.TUGAI);

    static final Ring R_REWRITE = new Ring(10, 0, 0);
    static final Ring R_SURFACE = new Ring(10, 0, 0, 2);

    /** 射线从顶面往下，只要最外层地表。 */
    static final Ray R_POOL = new Ray(Direction.DOWN, 1, 10, 6, 40);
    static final Ray R_TREE = new Ray(Direction.DOWN, 1, 10, 19, 40);
    static final Ray R_GRASS = new Ray(Direction.DOWN, 1, 10, 32, 40);

    public DesertToTugai() {
        addPhase(be -> forBS(be, R_REWRITE, CHANGER) & forPos(be, R_SURFACE, only(BiomeConstant.TUGAI, TugaiHelper::surface)));
        addPhase(be -> forPos(be, R_POOL, only(BiomeConstant.TUGAI, TugaiHelper::pool)));
        addPhase(be -> forPos(be, R_TREE, only(BiomeConstant.TUGAI, TugaiHelper::tree)));
        addPhase(be -> forPos(be, R_GRASS, only(BiomeConstant.TUGAI, TugaiHelper::grass)));
    }

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel() instanceof ServerLevel sl && sl.getBiome(be.getBlockPos()).is(Biomes.DESERT);
    }
}
