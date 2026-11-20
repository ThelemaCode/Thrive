package com.thelema.thrive.register;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * 木质类型的注册。
 *
 * <p>顺序要求：{@link BlockSetType} 必须先进 {@code BlockSetType.VALUES}，
 * {@link WoodType} 才能引用它——所以两者写在同一个类里、按声明顺序初始化。
 *
 * <p>注册名会成为贴图路径的一部分：告示牌的贴图是
 * {@code textures/entity/signs/<path>.png}，悬挂告示牌是
 * {@code textures/entity/signs/hanging/<path>.png} 和 {@code textures/gui/hanging_signs/<path>.png}。
 */
public class WoodTypeRegister {

    public static final BlockSetType DESERT_POPLAR_SET =
            BlockSetType.register(new BlockSetType("thrive:desert_poplar"));

    public static final WoodType DESERT_POPLAR =
            WoodType.register(new WoodType("thrive:desert_poplar", DESERT_POPLAR_SET));
}
