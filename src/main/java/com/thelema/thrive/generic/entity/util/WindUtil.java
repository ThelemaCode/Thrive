package com.thelema.thrive.generic.entity.util;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WindUtil {
    
    private WindUtil() {}

    /**
     * 对周围实体施加风力吸引。
     *
     * @param attractor       吸引源实体
     * @param radius          吸引半径
     * @param forceMultiplier 水平拉力基础系数
     * @param upwardForce     垂直向上浮力基础系数
     */
    public static void attractEntities(Entity attractor, double radius, double forceMultiplier, double upwardForce) {
        Level level = attractor.level();
        if (level.isClientSide) return;

        AABB area = attractor.getBoundingBox().inflate(radius);
        
        for (Entity e : level.getEntities(attractor, area)) {
            // 过滤旁观者、创造玩家、风系免疫生物
            if (e.isSpectator() || (e instanceof Player p && p.isCreative())) continue;
            if (e instanceof LivingEntity living && living.hasEffect(MobEffects.WIND_CHARGED)) continue;
            
            Vec3 diff = attractor.position().subtract(e.position());
            double dist = diff.length();
            
            if (dist > radius || dist < 0.1) continue;
            
            // 计算引力：距离越近引力越强（距离越远衰减）
            double att = 1.0 - 0.6 * (dist / radius);
            
            // 引力方向与强度
            Vec3 force = diff.normalize().scale(forceMultiplier * att).add(0, upwardForce * att, 0);
            
            Vec3 delta = e.getDeltaMovement().add(force);
            // 限制最大速度，防止把实体吸成瞬移
            if (delta.length() > 3.0) delta = delta.normalize().scale(3.0);
            
            e.setDeltaMovement(delta);
            
            // 客户端同步
            if (e instanceof Player player) {
                player.hurtMarked = true;
            } else {
                e.hasImpulse = true;
            }
        }
    }
}