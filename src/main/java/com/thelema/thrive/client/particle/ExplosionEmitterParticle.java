package com.thelema.thrive.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.HugeExplosionSeedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExplosionEmitterParticle extends HugeExplosionSeedParticle {

    private final ParticleType<ColorParticleOption> particleToSpawn;
    // 【修改 1】存储三个 float 颜色
    private final float r, g, b;

    protected ExplosionEmitterParticle(ClientLevel level, double x, double y, double z,
                                       ParticleType<ColorParticleOption> particleToSpawn, float r, float g, float b) {
        super(level, x, y, z);
        this.particleToSpawn = particleToSpawn;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void tick() {
        for (int i = 0; i < 6; i++) {
            double d0 = this.x + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
            double d1 = this.y + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;
            double d2 = this.z + (this.random.nextDouble() - this.random.nextDouble()) * 4.0;

            // 【修改 2】使用 float 创建 ColorParticleOption
            this.level.addParticle(
                    ColorParticleOption.create(this.particleToSpawn, this.r, this.g, this.b),
                    d0, d1, d2,
                    0.0, 0.0, 0.0
            );
        }

        this.age++;
        if (this.age == this.lifetime) {
            this.remove();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Provider(ParticleType<ColorParticleOption> particleToSpawn) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            // 【修改 3】提取 float 颜色
            return new ExplosionEmitterParticle(level, x, y, z, particleToSpawn, type.getRed(), type.getGreen(), type.getBlue());
        }
    }
}