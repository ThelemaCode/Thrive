package com.thelema.thrive.client;

import com.thelema.thrive.register.FluidRegister;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MixinToolClient {

    /**
     * 根据流体给粒子染色。
     *
     * @param particle 粒子实例
     * @param level    世界
     * @param x        粒子 X 坐标
     * @param y        粒子 Y 坐标
     * @param z        粒子 Z 坐标
     * @param yOffset  向下探查流体的偏移量（水花传 -0.1，气泡传 0）
     */
    public static void setColorByFluid(Particle particle, Level level, double x, double y, double z, double yOffset) {
        BlockPos pos = BlockPos.containing(x, y + yOffset, z);
        FluidState fs = level.getFluidState(pos);

        if (fs.getFluidType() == FluidRegister.LANTERN_BERRY_JUICE_TYPE.get()) {
            particle.setColor(1.0F, 0.88F, 0.2F); // 果汁：橙黄
        } else if (fs.getFluidType() == FluidRegister.SKY_WATER_TYPE.get()) {
            particle.setColor(0.5F, 0.8F, 1.0F); // 天空水：明亮天蓝
        } else {
            particle.setColor(0.25F, 0.46F, 0.89F); // 保底：深蓝色
        }


    }
}