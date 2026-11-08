package com.thelema.thrive.generic.blockentity;

import com.thelema.thrive.generic.blockentity.cap.Rotator;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifierRegistry;
import com.thelema.thrive.register.BlockEntityRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.runTick;

public class BiomeCoreBlockEntity extends BlockEntity {

    public final Rotator rotator = new Rotator(this);

    /**
     * 交互后启动的查表倒计时（tick）。0 = 未启动。
     */
    public int checkTimer = 0;

    /**
     * 当前运行的转换器 id。null = 空闲。
     */
    public ResourceLocation activeBiomeModifierId;

    /**
     * 通用NBT
     */
    public final CompoundTag data = new CompoundTag();

    public BiomeCoreBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegister.BIOME_CORE.get(), pos, state);
    }

    // ===== 交互逻辑 =====

    /**
     * 右键放入物品。
     * 每次放入 1 个，非创造模式会消耗玩家手中的物品。
     */
    public void insertItem(Player player, ItemStack stack) {
        // 转换中不许放
        if (stack.isEmpty() || activeBiomeModifierId != null) {
            return;
        }

        if (rotator.add(stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            resetCheckTimer();
        }
    }

    /**
     * 空手取出物品。
     * 拿出 rotator 列表中的最后一个（排序后的最后一个）。
     */
    public void extractItem(Player player) {
        // 转换中不许拿
        if (activeBiomeModifierId != null) {
            return;
        }

        ItemStack removed = rotator.removeLast();
        if (!removed.isEmpty()) {
            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
            resetCheckTimer();
        }
    }

    /**
     * 每次交互重置 5 秒倒计时 (100 tick)。
     */
    private void resetCheckTimer() {
        this.checkTimer = 100;
        this.setChanged();
    }

    // ===== 主 tick =====

    public static void tick(Level level, BlockPos pos, BlockState state, BiomeCoreBlockEntity be) {
        if (level.isClientSide) return;

        // 1. 转换中
        if (be.activeBiomeModifierId != null) {
            BiomeModifier c = BiomeModifierRegistry.get(be.activeBiomeModifierId);
            if (c != null && runTick(c, be)) {
                be.resetConversion();
            }
            return;
        }

        // 2. 空闲，检查倒计时归零
        if (be.checkTimer > 0 && --be.checkTimer == 0) {
            be.tryStartConversion();
        }
    }

    /**
     * 查表并启动转换。
     */
    private void tryStartConversion() {
        if (rotator.isEmpty()) {
            return;
        }

        // 提取当前物品的 List<Item>
        List<Item> itemList = new ArrayList<>();
        for (ItemStack stack : rotator.items) {
            itemList.add(stack.getItem());
        }

        // 遍历所有符合该物品列表的配方
        for (ResourceLocation id : BiomeModifierRegistry.getCandidates(itemList)) {
            BiomeModifier modifier = BiomeModifierRegistry.get(id);
            if (modifier != null && modifier.canUse(this)) {
                activeBiomeModifierId = id;
                this.setChanged();
                return;
            }
        }
    }

    /**
     * 清空转换状态：Rotator、计时、进度、转换器 id。
     */
    public void resetConversion() {
        rotator.clear();
        checkTimer = 0;
        data.getAllKeys().clear();
        activeBiomeModifierId = null;
        this.setChanged();
    }

    // ===== 网络与存档 =====

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        rotator.save(tag, registries);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        rotator.save(tag, regs);
        tag.putInt("CheckTimer", checkTimer);
        tag.put("data", data);
        if (activeBiomeModifierId != null) {
            tag.putString("ActiveConvertor", activeBiomeModifierId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        rotator.load(tag, regs);
        if (tag.contains("data")) {
            data.merge(tag.getCompound("data"));
        }
        checkTimer = tag.getInt("CheckTimer");
        if (tag.contains("ActiveConvertor")) {
            activeBiomeModifierId = ResourceLocation.parse(tag.getString("ActiveConvertor"));
        }
    }
}