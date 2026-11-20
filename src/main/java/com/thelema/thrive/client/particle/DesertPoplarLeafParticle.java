package com.thelema.thrive.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 飘落的胡杨叶。
 *
 * <p>行为对齐原版樱花树的花瓣：从树叶下方缓缓飘落，边落边自旋，落地或横向速度归零就消失。
 * 贴图是 4 帧随机取一张（{@code particles/desert_poplar_leaves.json}）。
 */
@OnlyIn(Dist.CLIENT)
public class DesertPoplarLeafParticle extends TextureSheetParticle {

    private float rotSpeed;
    private final float particleRandom;
    private final float spinAcceleration;

    protected DesertPoplarLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.setSprite(sprites.get(this.random.nextInt(12), 12));
        this.rotSpeed = (float) Math.toRadians(this.random.nextBoolean() ? -30.0 : 30.0);
        this.particleRandom = this.random.nextFloat();
        this.spinAcceleration = (float) Math.toRadians(this.random.nextBoolean() ? -5.0 : 5.0);
        this.lifetime = 300;
        this.gravity = 7.5E-4F;
        float size = this.random.nextBoolean() ? 0.05F : 0.075F;
        this.quadSize = size;
        this.setSize(size, size);
        this.friction = 1.0F;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
        }
        if (this.removed) return;

        float age = (float) (300 - this.lifetime);
        float progress = Math.min(age / 300.0F, 1.0F);
        double dx = Math.cos(Math.toRadians(this.particleRandom * 60.0F)) * 2.0 * Math.pow(progress, 1.25);
        double dz = Math.sin(Math.toRadians(this.particleRandom * 60.0F)) * 2.0 * Math.pow(progress, 1.25);
        this.xd += dx * 0.0025F;
        this.zd += dz * 0.0025F;
        this.yd -= this.gravity;

        if (!this.onGround) {
            this.rotSpeed += this.spinAcceleration / 20.0F;
            this.oRoll = this.roll;
            this.roll += this.rotSpeed / 20.0F;
        }
        this.move(this.xd, this.yd, this.zd);

        // 落地，或者刚开始飘就撞到东西（横速归零），都直接消失
        if (this.onGround || (this.lifetime < 299 && (this.xd == 0.0 || this.zd == 0.0))) {
            this.remove();
        }
        if (!this.removed) {
            this.xd *= this.friction;
            this.yd *= this.friction;
            this.zd *= this.friction;
        }
    }

    /** 客户端注册用：{@code event.registerSpriteSet(...)} 的工厂。 */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public net.minecraft.client.particle.Particle createParticle(@NotNull SimpleParticleType type,
                                                                   @NotNull ClientLevel level,
                                                                   double x, double y, double z,
                                                                   double xSpeed, double ySpeed, double zSpeed) {
            return new DesertPoplarLeafParticle(level, x, y, z, this.sprites);
        }
    }
}
