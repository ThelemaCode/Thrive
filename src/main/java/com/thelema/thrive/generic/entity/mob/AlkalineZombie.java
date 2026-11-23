package com.thelema.thrive.generic.entity.mob;

import com.thelema.thrive.generic.effect.Saponification;
import com.thelema.thrive.register.EffectRegister;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// 碱化僵尸
public class AlkalineZombie extends Zombie {

    private boolean canStrike = false;  // 可以突袭
    private int attackTick = 20; // 攻击计时

    public AlkalineZombie(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
    }

    public boolean doHurtTarget(Entity entity) {
        boolean flag = super.doHurtTarget(entity);
        if (flag && entity instanceof LivingEntity living) {
            boolean worsen = Saponification.worsen(living);
            // 没有一级，初始化
            if (!worsen) {
                living.addEffect(new MobEffectInstance(EffectRegister.SAPONIFICATION, 36000), this);
            }
        }
        return flag;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0f)
                .add(Attributes.ARMOR, 0.0f)
                .add(Attributes.MOVEMENT_SPEED, 0.08f);
    }

    @Override
    protected void customServerAiStep() {
        attackTick++;
        // 有目标
        if (getTarget() != null) {
            // 能否突袭
            if (canStrike) {
                getNavigation().moveTo(getTarget(), 5.0F);
            } else {
                // 正常接近：1.0 = 不做加成，用属性原值
                getNavigation().moveTo(getTarget(), 1.0F);
            }
            // 攻击
            if (attackTick >= 20 && distanceTo(getTarget()) < 2.25f) {
                doHurtTarget(getTarget());
                canStrike = false;
            }
        }
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            canStrike = true;
        }
        return hurt;
    }

    protected boolean isSunSensitive() {
        return false;
    }

}
