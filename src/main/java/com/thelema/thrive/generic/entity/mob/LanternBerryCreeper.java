package com.thelema.thrive.generic.entity.mob;

import com.thelema.thrive.generic.block.SunBerry;
import com.thelema.thrive.generic.entity.misc.PrimedLanternBerry;
import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.EffectRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LanternBerryCreeper extends Creeper implements GeoAnimatable {

    private int grow = 0;
    private int bloomTimer = 6000;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LanternBerryCreeper(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
        this.goalSelector.getAvailableGoals().removeIf(w -> w.getGoal() instanceof SwellGoal);
    }

    // ==================== GeckoLib ====================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 3, this::animate));
    }

    private <E extends GeoAnimatable> PlayState animate(AnimationState<E> event) {
        if (event.isMoving()) {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("walk"));
        } else {
            event.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return tickCount;
    }

    // ==================== 果汁buff的保护机制 ====================

    @Override
    public void ignite() {
        setRemainingFireTicks(200);
    }

    @Override
    public int getSwellDir() {
        return isOnFire() ? 1 : -1;
    }

    // ==================== 着火点燃 ====================

    @Override
    public void tick()  {
        // 如果离玩家太近
        if (!level().isClientSide) {
            LivingEntity target = getTarget();
            if (target instanceof Player && distanceToSqr(target) < 9.0 && hasLineOfSight(target)
            && !isInWater() && !hasEffect(EffectRegister.LANTERN_BERRY_JUICE)) {
                setRemainingFireTicks(200);
            }
        }

        super.tick();

        if (!level().isClientSide()) {
            if (grow >= 64) {
                // 1. 利用计时器初始值，只在刚达到 64 的第一 tick 清空一次 AI
                if (bloomTimer == 6000) {
                    goalSelector.getAvailableGoals().clear();
                    targetSelector.getAvailableGoals().clear();
                    setTarget(null);
                    getNavigation().stop();
                }

                // 2. 倒计时
                bloomTimer--;

                // 3. 时间到，寻找位置放置
                if (bloomTimer <= 0) {
                    BlockPos current = blockPosition();
                    for (int dy = 0; dy <= 1; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                BlockPos targetPos = current.offset(dx, dy, dz);
                                if (SunBerry.tryPlaceSunBerry(level(), targetPos)) {
                                    discard(); // 放置成功后移除自身
                                    return;
                                }
                            }
                        }
                    }
                    // 如果 5 分钟到了，但周围没有空间，就每 10s 重试
                    bloomTimer = 200;
                }
            }
        }
    }

    // ==================== 火焰死亡也炸 ====================

    @Override
    public void die(DamageSource source) {
        if (isOnFire() || source.is(DamageTypeTags.IS_FIRE)) {
            fireExplode();
        }
        super.die(source);
    }

    // ==================== 爆炸：爆燃 ====================

    /** 由 CreeperMixin 在父类 explodeCreeper 的 HEAD 处调用，替代原版爆炸。 */
    public void fireExplode() {
        if (level() instanceof ServerLevel) {
            dead = true;

            gameEvent(GameEvent.ENTITY_DIE);

            discard();

            PrimedLanternBerry.deflagrate(
                    level(),
                    blockPosition(),
                    this,
                    this,
                    20.0F + grow * 3
            );

        }
    }


    // ==================== 右键交互 ====================

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 判断：必须是高压状态，且手里拿着灯笼浆果
        if (isPowered() && itemstack.is(BlockRegister.LANTERN_BERRY.get().asItem())) {
            // 如果 grow 还没满 64
            if (grow < 64) {
                grow++;

                // 播放进食/使用音效
                playSound(SoundEvents.GENERIC_EAT, 0.5F, random.nextFloat() * 0.1F + 0.9F);

                // 骨粉催熟粒子（作物生长粒子）
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(
                            ParticleTypes.COMPOSTER, // 替换为作物生长粒子
                            getX(), getY() + getBbHeight() * 0.5, getZ(),
                            8,                 // 数量
                            0.5, 0.5, 0.5,     // 偏移半径
                            0.0                // 速度
                    );
                }

                // 扣除物品（如果不是创造模式）
                if (!player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }

        // 如果不是上述情况，走原版逻辑（比如用打火石点燃）
        return super.mobInteract(player, hand);
    }

    // ==================== NBT 存档 ====================
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Grow", grow);
        compound.putInt("BloomTimer", bloomTimer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        grow = compound.getInt("Grow");
        bloomTimer = compound.getInt("BloomTimer");
    }
}