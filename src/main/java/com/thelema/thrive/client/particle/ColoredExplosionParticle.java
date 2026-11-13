package com.thelema.thrive.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColoredExplosionParticle extends HugeExplosionParticle {

    public ColoredExplosionParticle(ClientLevel level, double x, double y, double z,
                                    double quadSizeMultiplier, SpriteSet spriteSet,
                                    float r, float g, float b) {
        // HugeExplosionParticle 把 quadSizeMultiplier 当作大小缩放
        super(level, x, y, z, quadSizeMultiplier, spriteSet);

        // 原版 super() 里给了随机的灰度亮度
        float brightness = this.rCol;

        // 目标颜色 × 随机亮度
        this.setColor(r * brightness, g * brightness, b * brightness);
    }

    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet spriteSet) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {

            // 解包颜色
            float r = type.getRed();
            float g = type.getGreen();
            float b = type.getBlue();

            // 【修复】删掉 ySpeed 和 zSpeed，把 xSpeed 传给了大小乘数
            return new ColoredExplosionParticle(level, x, y, z, xSpeed, spriteSet, r, g, b);
        }
    }
}