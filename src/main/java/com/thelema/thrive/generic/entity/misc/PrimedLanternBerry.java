package com.thelema.thrive.generic.entity.misc;

import com.thelema.thrive.generic.tool.Damage;
import com.thelema.thrive.generic.tool.Knockback;
import com.thelema.thrive.register.EntityRegister;
import com.thelema.thrive.register.ParticleRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public class PrimedLanternBerry extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(PrimedLanternBerry.class, EntityDataSerializers.INT);
    private static final int DEFAULT_FUSE_TIME = 80;
    public static final String TAG_FUSE = "fuse";

    @Nullable
    private LivingEntity owner;

    public PrimedLanternBerry(EntityType<? extends PrimedLanternBerry> entityType, Level level) {
        super(entityType, level);
        blocksBuilding = true;
    }

    public PrimedLanternBerry(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(EntityRegister.PRIMED_LANTERN_BERRY.get(), level);
        setPos(x, y, z);
        double d0 = level.random.nextDouble() * Math.PI * 2;
        setDeltaMovement(-Math.sin(d0) * 0.02, 0.2, -Math.cos(d0) * 0.02);
        setFuse(DEFAULT_FUSE_TIME);
        xo = x;
        yo = y;
        zo = z;
        this.owner = owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE_ID, DEFAULT_FUSE_TIME);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        applyGravity();
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.98));
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }

        int i = getFuse() - 1;
        setFuse(i);
        if (i <= 0) {
            if (level() instanceof ServerLevel sl) {
                deflagrate(sl, blockPosition(), null, this, 20.0F);
            }
            discard();
        } else {
            updateInWaterStateAndDoFluidPushing();
        }
    }

    public static void deflagrate(Level level, BlockPos pos, @Nullable LivingEntity owner, @Nullable Entity excludeEntity, float damage) {
        // 服务端
        if (level instanceof ServerLevel sl) {

            // 点燃周围
            for (BlockPos b : BlockPos.betweenClosed(pos.offset(-4, -4, -4), pos.offset(4, 4, 4))) {
                BlockState state = level.getBlockState(b);
                state.onCaughtFire(sl, b, null, null);
            }

            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;

            sl.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.8F);
            sl.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.8F);


            sl.sendParticles(
                    // 【核心修改】用 ColorParticleOption 包装目标粒子类型和火焰色
                    ColorParticleOption.create(ParticleRegister.COLORED_EXPLOSION_EMITTER.get(), 0xFFA525),
                    cx, cy, cz,
                    1,              // 只发 1 颗发射器
                    0.0, 0.0, 0.0,  // 无偏移
                    0.0             // 无速度（发射器本身不移动）
            );

            // 一次调用发 300 颗，球状分布
            sl.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 300, 1.5, 1.5, 1.5, 0.3);

            AABB area = new AABB(
                    pos.getX() - 4.5, pos.getY() - 4.5, pos.getZ() - 4.5,
                    pos.getX() + 5.5, pos.getY() + 5.5, pos.getZ() + 5.5
            );

            // 伤害 Living并点燃
            sl.getEntitiesOfClass(LivingEntity.class, area).forEach(entity -> {
                if (entity == excludeEntity) return;
                Damage.hurt(entity, DamageTypes.ON_FIRE, damage);
                entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 200);
            });

            // 击退
            Knockback.push(sl, pos.getCenter(), 1.5f, 1.0f, e -> true);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putShort(TAG_FUSE, (short) getFuse());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        setFuse(compound.getShort(TAG_FUSE));
    }

    @Nullable
    public LivingEntity getOwner() {
        return owner;
    }

    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        if (entity instanceof PrimedLanternBerry berry) {
            owner = berry.owner;
        }
    }

    public void setFuse(int fuse) {
        entityData.set(DATA_FUSE_ID, fuse);
    }

    public int getFuse() {
        return entityData.get(DATA_FUSE_ID);
    }
}