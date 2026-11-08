package com.thelema.thrive.generic.entity.mob;

import com.thelema.thrive.generic.tool.Damage;
import com.thelema.thrive.register.constant.DamageTypeConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class BreezeCore extends Mob implements GeoEntity {

    public static final int R = 9;

    private static final int WATER_TICKS_TO_ATTACH = 200; // 10 秒
    private static final int FIRE_TICKS_TO_ATTACH = 200; // 10 秒
    private static final int SMELT_INTERVAL = 40; // 熔炉 200 tick ÷ 5

    /**
     * 附着机制：0 = 默认，1 = 水，2 = 火。同步到客户端供渲染层染色。
     */
    private static final EntityDataAccessor<Integer> DATA_FLAG =
            SynchedEntityData.defineId(BreezeCore.class, EntityDataSerializers.INT);

    /**
     * 每个掉落物的烧炼进度（tick）。
     */
    private final Map<UUID, Integer> smeltProgress = new HashMap<>();
    /**Home坐标*/
    private BlockPos homePos = null;


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private BlockPos wanderTarget;
    private int wanderCooldown;

    /**
     * 连续满足水条件的 tick 数，中断即清零。
     */
    private int waterTicks = 0;
    /**
     * 连续燃烧的 tick 数，中断即清零。
     */
    private int fireTicks = 0;

    /**
     * 旋风音循环倒计时。0 = 重新随机，>0 = 递减。
     */
    private int soundTick = 0;

    public BreezeCore(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public int getFlag() {
        return this.entityData.get(DATA_FLAG);
    }

    public void setFlag(int flag) {
        this.entityData.set(DATA_FLAG, flag);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAG, 0);
    }

    // ───── GeckoLib 动画 ─────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "controller", 3, this::animate));
    }

    private <E extends GeoAnimatable> PlayState animate(AnimationState<E> event) {
        event.getController().setAnimation(RawAnimation.begin().thenLoop("rotation"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected void customServerAiStep() {
        // 设置 Home
        if (homePos == null) {
            homePos = blockPosition().immutable();
        }

        // ── 水附着：被雨淋或处于水中，连续 10s ──
        if (this.isInWaterRainOrBubble()) {
            if (++waterTicks >= WATER_TICKS_TO_ATTACH && getFlag() == 0) {
                setFlag(1);
            }
        } else {
            waterTicks = 0;
        }

        // ── 火附着：持续燃烧 10s（可覆盖水）──
        if (this.isOnFire() || this.getRemainingFireTicks() > 0) {
            if (++fireTicks >= FIRE_TICKS_TO_ATTACH && getFlag() != 2) {
                setFlag(2);
            }
        } else {
            fireTicks = 0;
        }

        Vec3 center = this.getBoundingBox().getCenter();
        boolean damageTick = this.tickCount % 20 == 0;

        Holder<DamageType> windHolder = damageTick
                ? level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypeConstant.WIND_GROUND)
                : null;

        for (Entity e : this.level().getEntitiesOfClass(
                Entity.class, this.getBoundingBox().inflate(R))) {

            if (e == this || e.isSpectator() ||
                    (e instanceof ServerPlayer p && p.isCreative())) continue;

            Vec3 diff = center.subtract(e.position());
            double dist = diff.length();
            if (dist > R) continue;

            // ── 引力（线性衰减）──
            double att = 1.0 - 0.7 * (dist / R);
            Vec3 force = diff.normalize().scale(0.6 * att).add(0, -0.05 * att, 0);
            if (!(e instanceof BreezeCore)) {
                force = force.add(0, 0.20 * att, 0);
            }

            Vec3 delta = e.getDeltaMovement().add(force);
            if (delta.length() > 3.0) delta = delta.normalize().scale(3.0);

            e.setDeltaMovement(delta);
            e.fallDistance = 0;

            if (e instanceof Player player) player.hurtMarked = true;
            else e.hasImpulse = true;

            // ── 风场伤害 ──
            if (damageTick && e instanceof LivingEntity living) {
                Damage.hurt(living, new DamageSource(windHolder, this, this), 2.0F);

                if (getFlag() == 1) {
                    // 水伤
                    Damage.hurt(living, DamageTypes.DROWN, 2.0F);
                    living.clearFire();
                } else if (getFlag() == 2) {
                    // 火伤
                    Damage.hurt(living, DamageTypes.ON_FIRE, 2.0F);
                    living.setRemainingFireTicks(living.getRemainingFireTicks() + 100);
                }
            }
        }

        // ── 熔炼（移出实体循环，每 tick 只跑一次）──
        if (getFlag() == 2 && level() instanceof ServerLevel sl) {
            smeltItems(sl);
        }

        // ── 游荡 ──
        if (wanderTarget == null
                || --wanderCooldown <= 0
                || blockPosition().distSqr(wanderTarget) < 16) {
            int tx = blockPosition().getX() + random.nextInt(33) - 16;
            int ty = Mth.clamp(blockPosition().getY() + random.nextInt(17) - 8,
                    level().getMinBuildHeight() + 1,
                    level().getMaxBuildHeight() - 2);
            int tz = blockPosition().getZ() + random.nextInt(33) - 16;
            BlockPos potentialTarget = new BlockPos(tx, ty, tz);
            // 检查该点是否离 homePos 太远
            if (homePos.distSqr(potentialTarget) > 80 * 80) {
                // 如果太远，则放弃本次随机，直接把目标设为 homePos，让其往回飞
                wanderTarget = homePos;
            } else {
                wanderTarget = potentialTarget;
            }
            wanderCooldown = 80 + random.nextInt(80);
        }

        this.moveControl.setWantedPosition(
                wanderTarget.getX() + 0.5,
                wanderTarget.getY() + 0.5,
                wanderTarget.getZ() + 0.5,
                0.5);
    }

    // ───── 熔炼 ─────
    private void smeltItems(ServerLevel sl) {
        AABB area = AABB.ofSize(getBoundingBox().getCenter(), R * 2, R * 2, R * 2);
        Set<UUID> seen = new HashSet<>();

        for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, area)) {
            UUID id = ie.getUUID();
            seen.add(id);

            int t = smeltProgress.merge(id, 1, Integer::sum);
            if (t < SMELT_INTERVAL) continue;
            smeltProgress.put(id, t - SMELT_INTERVAL);

            ItemStack stack = ie.getItem();
            sl.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), sl)
                    .ifPresent(rh -> {
                        ItemStack out = rh.value().getResultItem(sl.registryAccess()).copy();
                        out.setCount(stack.getCount());
                        ie.setItem(out);
                    });
        }

        smeltProgress.keySet().retainAll(seen);
    }

    // ───── 拉动机制 ─────
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    // ───── 风声 ─────
    @Override
    public void tick() {
        // 与原版 Breeze.tick 一致：每 1~80 tick 随机播一次旋风音
        this.soundTick = this.soundTick == 0
                ? this.random.nextIntBetweenInclusive(1, 80)
                : this.soundTick - 1;
        if (this.soundTick == 0) {
            this.playWhirlSound();
        }
        super.tick();
    }

    public void playWhirlSound() {
        float pitch = 0.7F + 0.4F * this.random.nextFloat();
        float volume = 0.8F + 0.2F * this.random.nextFloat();
        this.level().playLocalSound(this, SoundEvents.BREEZE_WHIRL, this.getSoundSource(), volume, pitch);
    }

    // ───── 声音 ─────

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BREEZE_IDLE_AIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BREEZE_DEATH;
    }

    // ───── 摔落免伤 ─────

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState s, BlockPos p) {
    }

    // ───── 附着带来的免伤 ─────
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (getFlag() == 1 && source.is(DamageTypes.DROWN)) {
            return true;
        }
        if (getFlag() == 2 && source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    // ───── 属性 ─────

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.5);
    }

    // ───── 序列化 ─────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Flag", getFlag());
        tag.putInt("WaterTicks", waterTicks);
        tag.putInt("FireTicks", fireTicks);
        if (homePos != null) {
            tag.putLong("HomePos", homePos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFlag(tag.getInt("Flag"));
        waterTicks = tag.getInt("WaterTicks");
        fireTicks = tag.getInt("FireTicks");
        if (tag.contains("HomePos")) {
            homePos = BlockPos.of(tag.getLong("HomePos"));
        }
    }
}