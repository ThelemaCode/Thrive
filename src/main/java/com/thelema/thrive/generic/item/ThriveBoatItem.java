package com.thelema.thrive.generic.item;

import com.thelema.thrive.generic.entity.misc.ThriveBoat;
import com.thelema.thrive.generic.entity.misc.ThriveChestBoat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

/**
 * 船物品。
 *
 * <p>原版 {@code BoatItem} 的 {@code getBoat(...)} 是 private、且固定 new 原版的
 * {@code Boat}/{@code ChestBoat}，没法复用，所以这里把 {@code use} 复制了一份，
 * 只把「造哪个实体」换掉。行为（水面判定、碰撞检查、统计、音效事件）与原版一致。
 */
public class ThriveBoatItem extends Item {

    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);

    private final boolean chest;

    public ThriveBoatItem(boolean chest, Properties properties) {
        super(properties);
        this.chest = chest;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 view = player.getViewVector(1.0F);
        List<Entity> targets = level.getEntities(player,
                player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0), ENTITY_PREDICATE);
        if (!targets.isEmpty()) {
            Vec3 eye = player.getEyePosition();
            for (Entity entity : targets) {
                AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (box.contains(eye)) {
                    return InteractionResultHolder.pass(stack);
                }
            }
        }

        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 at = hit.getLocation();
        Boat boat = this.chest
                ? new ThriveChestBoat(level, at.x, at.y, at.z)
                : new ThriveBoat(level, at.x, at.y, at.z);
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox())) {
            return InteractionResultHolder.fail(stack);
        }

        if (level instanceof ServerLevel serverLevel) {
            EntityType.<Boat>createDefaultStackConfig(serverLevel, stack, player).accept(boat);
            serverLevel.addFreshEntity(boat);
            serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
