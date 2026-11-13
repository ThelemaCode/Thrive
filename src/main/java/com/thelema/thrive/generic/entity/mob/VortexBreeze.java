package com.thelema.thrive.generic.entity.mob;

import com.thelema.thrive.generic.block.LanternBerry;
import com.thelema.thrive.generic.block.SunBerry;
import com.thelema.thrive.generic.entity.util.WindUtil;
import com.thelema.thrive.generic.tool.Damage;
import com.thelema.thrive.register.EffectRegister;
import com.thelema.thrive.register.constant.DamageTypeConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class VortexBreeze extends Mob implements GeoEntity {

    public static final int R = 9;

    private static final int WATER_TICKS_TO_ATTACH = 200; // 水附着所需积累 10 秒
    private static final int FIRE_TICKS_TO_ATTACH = 200; // 火附着所需积累 10 秒
    private static final int SNOW_TICKS_TO_ATTACH = 200; // 雪附着所需积累 10 秒
    private static final int SMELT_INTERVAL = 40; // 熔炼间隔（熔炉 200 tick 的 5 倍速）

    /**
     * 附着机制：0 = 默认，1 = 水，2 = 火，3 = 雷雨，4 = 雪。
     * 同步到客户端供渲染层染色。
     */
    private static final EntityDataAccessor<Integer> DATA_FLAG =
            SynchedEntityData.defineId(VortexBreeze.class, EntityDataSerializers.INT);

    /**
     * 每个掉落物的烧炼进度（tick）。
     */
    private final Map<UUID, Integer> smeltProgress = new HashMap<>();

    /**
     * 生成时的初始坐标，游荡时以此为中心限制范围。
     */
    private BlockPos homePos = null;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private BlockPos wanderTarget;
    private int wanderCooldown;

    /**
     * 连续满足水/火/雪条件的 tick 数，中断即缓慢递减。
     */
    private int waterTicks = 0;
    private int fireTicks = 0;
    private int snowTicks = 0;

    /**
     * 旋风音循环倒计时。0 = 重新随机，>0 = 递减。
     */
    private int soundTick = 0;

    public VortexBreeze(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // 飞行实体无重力
        this.moveControl = new FlyingMoveControl(this, 20, true); // 使用飞行移动控制器
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

    //  ───── 主AI ─────
    @Override
    protected void customServerAiStep() {
        // 记录初始位置作为 Home
        if (homePos == null) {
            homePos = blockPosition().immutable();
        }

        // ── 水附着：被雨淋或处于水中，积累 10s ──
        if (this.isInWaterRainOrBubble()) {
            this.waterTicks++;
            if (this.waterTicks >= WATER_TICKS_TO_ATTACH && getFlag() == 0) {
                setFlag(1);
            }
        } else if (this.waterTicks > 0) {
            this.waterTicks--; // 缓慢减少
        }

        // ── 火附着：持续燃烧 10s（可覆盖水）──
        if (this.isOnFire() || this.getRemainingFireTicks() > 0) {
            this.fireTicks++;
            if (this.fireTicks >= FIRE_TICKS_TO_ATTACH && getFlag() != 2) {
                setFlag(2);
            }
        } else if (this.fireTicks > 0) {
            this.fireTicks--; // 缓慢减少
        }

        // ── 雪附着：处于细雪中，积累 10s ──
        if (this.isInPowderSnow) {
            this.snowTicks++;
            if (this.snowTicks >= SNOW_TICKS_TO_ATTACH && getFlag() != 4) {
                setFlag(4);
            }
        } else if (this.snowTicks > 0) {
            this.snowTicks--; // 缓慢减少
        }

        // ── 火状态特殊逻辑 ──
        if (getFlag() == 2 && level() instanceof ServerLevel sl) {
            // 喝了果汁就持续燃烧
            if (getEffect(EffectRegister.LANTERN_BERRY_JUICE) != null) {
                setRemainingFireTicks(1000);
            }
            // 每秒点燃周围可燃物
            if (this.tickCount % 20 == 0) {
                igniteAround(sl);
            }
        }

        // ── 调用工具类：施加风场引力 ──
        // 半径 R，水平力 0.6，垂直上浮力 0.15
        WindUtil.attractEntities(this, R, 0.6, 0.15);

        // ── 伤害判定：每秒执行一次 ──
        if (this.tickCount % 20 == 0) {
            applyDamage();
        }

        // ── 熔炼：每 tick 只跑一次 ──
        if (getFlag() == 2 && level() instanceof ServerLevel sl) {
            smeltItems(sl);
        }

        // ── 游荡逻辑 ──
        if (wanderTarget == null
                || --wanderCooldown <= 0
                || blockPosition().distSqr(wanderTarget) < 16) {
            
            int tx = blockPosition().getX() + random.nextInt(33) - 16;
            int ty = Mth.clamp(blockPosition().getY() + random.nextInt(17) - 8,
                    level().getMinBuildHeight() + 1,
                    level().getMaxBuildHeight() - 2);
            int tz = blockPosition().getZ() + random.nextInt(33) - 16;
            
            BlockPos potentialTarget = new BlockPos(tx, ty, tz);
            
            // 距离 Home 超过 80 格时，强制往回飞
            if (homePos.distSqr(potentialTarget) > 80 * 80) {
                wanderTarget = homePos;
            } else {
                wanderTarget = potentialTarget;
            }
            wanderCooldown = 80 + random.nextInt(80);
        }

        // 驱动飞行
        this.moveControl.setWantedPosition(
                wanderTarget.getX() + 0.5,
                wanderTarget.getY() + 0.5,
                wanderTarget.getZ() + 0.5,
                0.5);
    }

    // ───── 伤害判定（独立方法） ─────
    private void applyDamage() {
        Holder<DamageType> windHolder = level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypeConstant.WIND_GROUND);

        for (Entity e : this.level().getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(R))) {
            // 过滤自身、旁观者、创造玩家、原版旋风人、蓄风生物
            if (e == this
                    || e.isSpectator()
                    || (e instanceof ServerPlayer p && p.isCreative())
                    || e instanceof Breeze
                    || (e instanceof LivingEntity living && living.hasEffect(MobEffects.WIND_CHARGED))
            ) continue;

            if (this.distanceToSqr(e) > R * R) continue;

            if (e instanceof LivingEntity living) {
                // 基础风伤（2 点）
                Damage.hurt(living, new DamageSource(windHolder, this, this), 2.0F);

                if (getFlag() == 1) {
                    // 水附着：2 点溺水 + 灭火
                    Damage.hurt(living, DamageTypes.DROWN, 2.0F);
                    living.clearFire();
                } else if (getFlag() == 2) {
                    // 火附着：2 点火伤 + 续燃
                    Damage.hurt(living, DamageTypes.ON_FIRE, 2.0F);
                    living.setRemainingFireTicks(living.getRemainingFireTicks() + 100);
                } else if (getFlag() == 3) {
                    // 雷雨附着：3 点水伤 + 3 点雷伤
                    Damage.hurt(living, DamageTypes.DROWN, 3.0F);
                    Damage.hurt(living, DamageTypes.LIGHTNING_BOLT, 3.0F);
                    // 25% 概率召唤真实雷电
                    if (this.random.nextFloat() < 0.25F) {
                        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level());
                        if (bolt != null) {
                            bolt.moveTo(living.position());
                            level().addFreshEntity(bolt);
                        }
                    }
                } else if (getFlag() == 4) {
                    // 雪附着：2 点冰冻 + 迅速冻结
                    Damage.hurt(living, DamageTypes.FREEZE, 2.0F);
                    living.setTicksFrozen(living.getTicksFrozen() + 100);
                }
            }
        }
    }

    // ───── 雷击转化 ─────
    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        // 直接被雷劈时，立刻切换到雷雨状态
        setFlag(3);
        this.clearFire(); // 防止原版逻辑让其着火变成火状态
    }

    // ───── 火状态点燃周围 ─────
    private void igniteAround(ServerLevel sl) {
        AABB area = this.getBoundingBox().inflate(3.0);
        BlockPos minPos = BlockPos.containing(area.minX, area.minY, area.minZ);
        BlockPos maxPos = BlockPos.containing(area.maxX, area.maxY, area.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = sl.getBlockState(pos);

            // 1. 特殊爆炸物：触发点燃并移除原方块
            if (state.getBlock() instanceof TntBlock
                    || state.getBlock() instanceof LanternBerry
                    || state.getBlock() instanceof SunBerry) {
                state.onCaughtFire(sl, pos, null, this);
                sl.removeBlock(pos, false);
                continue;
            }

            // 2. 普通可燃物：在顶部生成火
            if (state.isFlammable(sl, pos, Direction.UP) && sl.isEmptyBlock(pos.above())) {
                sl.setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState());
            }
        }

        // 3. 点燃周围的生物和掉落物
        List<Entity> entities = sl.getEntities(this, this.getBoundingBox().inflate(3.0));
        for (Entity e : entities) {
            if (e instanceof LivingEntity living && !living.fireImmune()) {
                living.igniteForSeconds(5);
            } else if (e instanceof ItemEntity item && !item.fireImmune()) {
                item.igniteForSeconds(5);
            }
        }
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
        int flag = getFlag();
        if (flag == 1 && source.is(DamageTypes.DROWN)) {
            return true;
        }
        if (flag == 2 && source.is(DamageTypeTags.IS_FIRE)) {
            return true;
        }
        if (flag == 3 && (source.is(DamageTypes.LIGHTNING_BOLT) || source.is(DamageTypes.DROWN))) {
            return true;
        }
        if (flag == 4 && source.is(DamageTypeTags.IS_FREEZING)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    // ───── 物理伤害抗性（NeoForge 1.21 必须操作 DamageContainer） ─────
    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float damage) {
        // 1. 必须调用 super 让原版的药水抗性和附魔保护先生效
        damage = super.getDamageAfterMagicAbsorb(source, damage);

        // 2. 检查是否是物理伤害
        if (source.is(Tags.DamageTypes.IS_PHYSICAL)) {
            // 3. 确保容器存在
            if (this.damageContainers != null && !this.damageContainers.isEmpty()) {
                DamageContainer container = this.damageContainers.peek();
                float currentDamage = container.getNewDamage(); // 拿到当前的最新伤害

                // 4. 真正把伤害砍掉 85%，写入容器
                container.setNewDamage(currentDamage * 0.15F);

                // 同步返回，保持一致
                damage = currentDamage * 0.15F;
            }
        }
        return damage;
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
        tag.putInt("SnowTicks", snowTicks);
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
        snowTicks = tag.getInt("SnowTicks");
        if (tag.contains("HomePos")) {
            homePos = BlockPos.of(tag.getLong("HomePos"));
        }
    }
}