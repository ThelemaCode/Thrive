package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;

/**
 * 群系修改器接口
 */
public interface BiomeModifier {

    boolean canUse(BiomeCoreBlockEntity be);

    /**
     * 本 tick 处理。返回 true = 本次 forPos 遍历结束，BE 自行决定是否收尾。
     */
    boolean tick(BiomeCoreBlockEntity be);

}