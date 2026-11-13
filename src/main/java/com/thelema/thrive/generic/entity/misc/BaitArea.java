package com.thelema.thrive.generic.entity.misc;

import com.thelema.thrive.register.FluidRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class BaitArea extends Entity {

    @Nullable
    public ResourceLocation baitItemId = null; // 鱼饵物品ID
    public int lifespan = 12000; // 10分钟

    public BaitArea(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            this.lifespan--;
            if (this.lifespan <= 0) {
                this.discard();
                return;
            }
        }

        if (level() instanceof ServerLevel serverLevel) {
            if (this.random.nextInt(4) == 0) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double dist = this.random.nextDouble() * 6.0;
                double px = this.getX() + Math.cos(angle) * dist;
                double pz = this.getZ() + Math.sin(angle) * dist;

                for (int dy = 3; dy >= -3; dy--) {
                    BlockPos checkPos = BlockPos.containing(px, this.getY() + dy, pz);
                    FluidState fs = level().getFluidState(checkPos);

                    if (fs.getFluidType() == FluidRegister.LANTERN_BERRY_JUICE_TYPE.get()
                            && level().getBlockState(checkPos.above()).isAir()) {

                        double surfaceY = checkPos.getY() + fs.getHeight(level(), checkPos) + 0.1;

                        serverLevel.sendParticles(
                                ParticleTypes.SPORE_BLOSSOM_AIR,
                                checkPos.getX() + 0.5, surfaceY, checkPos.getZ() + 0.5,
                                3,
                                0.4, 0.2, 0.4,
                                0.02
                        );
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BaitItemId")) {
            this.baitItemId = ResourceLocation.parse(tag.getString("BaitItemId"));
        }
        if (tag.contains("Lifespan")) {
            this.lifespan = tag.getInt("Lifespan");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.baitItemId != null) {
            tag.putString("BaitItemId", this.baitItemId.toString());
        }
        tag.putInt("Lifespan", this.lifespan);
    }
}