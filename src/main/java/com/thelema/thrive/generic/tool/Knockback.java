package com.thelema.thrive.generic.tool;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

public class Knockback {

    /**
     * 对以给定点为中心、半径内的实体施加爆炸式击退（向外推）。
     */
    public static List<Entity> push(ServerLevel level, Vec3 center, double radius, double strength,
                                    Function<Entity, Boolean> predicate) {
        AABB aabb = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, aabb,
                e -> e.isAlive() && predicate.apply(e));

        for (Entity entity : entities) {
            double dx = entity.getX() - center.x;
            double dy = entity.getY() - center.y;
            double dz = entity.getZ() - center.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 0.01) continue;
            double factor = Math.max(0, strength * (1.0 - distance / radius));
            dx /= distance;
            dy /= distance;
            dz /= distance;
            // 添加垂直分量，产生弹跳效果
            entity.setDeltaMovement(entity.getDeltaMovement().add(dx * factor, dy * factor + 0.2, dz * factor));
            entity.hurtMarked = true;
        }
        return entities;
    }

    /**
     * 对以给定点为中心、半径内的实体施加一次逆向牵引（向内拉）。
     */
    public static List<Entity> pull(ServerLevel level, Vec3 center, double radius, double strength,
                                    Function<Entity, Boolean> predicate) {
        AABB aabb = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, aabb,
                e -> e.isAlive() && predicate.apply(e));

        for (Entity entity : entities) {
            // 向量从实体指向中心，产生吸力
            double dx = center.x - entity.getX();
            double dy = center.y - entity.getY();
            double dz = center.z - entity.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // 距离太近的忽略，防止直接重合导致速度爆表
            if (distance < 0.1) continue;

            // 距离越近，吸力越强
            double factor = Math.max(0, strength * (1.0 - distance / radius));
            dx /= distance;
            dy /= distance;
            dz /= distance;

            // 添加轻微垂直分量，让被吸过来的实体有个抛物线
            entity.setDeltaMovement(entity.getDeltaMovement().add(dx * factor, dy * factor + 0.1, dz * factor));
            entity.hurtMarked = true;
        }
        return entities;
    }
}