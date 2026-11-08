package com.thelema.thrive.generic.blockentity.cap;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 浮空环绕展示能力。
 * 包含多个物品（上限 8 个），支持椭圆/椭球轨道与多轴自转。
 */
public class Rotator {

    public final BlockEntity owner;

    /** 环绕展示的物品，每个物品独立存在，自动按注册 ID 排序。 */
    public final List<ItemStack> items = new ArrayList<>();

    /** 最大容纳物品数 */
    public static final int MAX_ITEMS = 8;

    /** 中心点相对于方块中心的偏移量 */
    public float ox = 0f, oy = 1.0f, oz = 0f;

    /** 物品自转角速度 (度/秒) */
    public float rx = 180f, ry = 180f, rz = 180f;

    /** 物品环绕中心的角速度 (度/秒) */
    public float crx = 0f, cry = 90f, crz = 0f;

    /** 椭圆轨道半径（X/Z 控制水平椭圆，Y 用于 3D 翻转时的厚度） */
    public float radiusX = 0.5f, radiusY = 0.5f, radiusZ = 0.5f;

    public Rotator(BlockEntity owner) {
        this.owner = owner;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isFull() {
        return items.size() >= MAX_ITEMS;
    }

    /** 放入物品，自动排序 */
    public boolean add(ItemStack stack) {
        if (stack.isEmpty() || isFull()) return false;
        items.add(stack.copyWithCount(1));
        sortItems();
        owner.setChanged();
        sync();
        return true;
    }

    /** 取出物品（默认取出列表中的最后一个，配合排序即字典序最大的） */
    public ItemStack removeLast() {
        if (items.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = items.remove(items.size() - 1);
        owner.setChanged();
        sync();
        return removed;
    }

    /** 清空容器，不掉落 */
    public void clear() {
        items.clear();
        owner.setChanged();
        sync();
    }

    /** 掉落所有物品 */
    public void dropAll() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        for (ItemStack stack : items) {
            Block.popResource(level, owner.getBlockPos().above(), stack);
        }
        clear();
    }

    /** 基于物品注册 ID 排序，保证查询匹配的一致性 */
    public void sortItems() {
        items.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    // ===== NBT 持久化 =====

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            list.add(stack.save(registries));
        }
        tag.put("RotatorItems", list);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        items.clear();
        if (tag.contains("RotatorItems", Tag.TAG_LIST)) {
            ListTag list = tag.getList("RotatorItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack.parse(registries, list.getCompound(i)).ifPresent(items::add);
            }
            sortItems(); // 读档后确保顺序一致
        }
    }

    // ===== 客户端同步 =====

    private void sync() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3);
    }
}