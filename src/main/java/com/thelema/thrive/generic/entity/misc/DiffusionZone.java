package com.thelema.thrive.generic.entity.misc;

import com.thelema.thrive.register.EntityRegister;
import com.thelema.thrive.register.FluidRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * 扩散带：指某种物质分散在流体中而形成的区域
 * */
public class DiffusionZone extends Entity {

    @Nullable
    public ResourceLocation diffusateItemId = null; // 扩散物 ID
    public int lifespan = 100;
    @Nullable
    public ParticleOptions particle;

    public DiffusionZone(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    // 生命周期初始值：
    // 为负数，只能通过方块覆盖而消失
    // 为正数，计时消失
    public static void spawnInWorld(ServerLevel sl, BlockPos pos, ResourceLocation itemId, int lifespan, @Nullable ParticleOptions particle) {
        DiffusionZone zone = new DiffusionZone(EntityRegister.DIFFUSION_ZONE.get(), sl);
        zone.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        zone.diffusateItemId = itemId;
        zone.lifespan = lifespan;
        zone.particle = particle;
        sl.addFreshEntity(zone);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel sl) {

            // 生命周期逻辑
            this.lifespan--;
            if (this.lifespan == 0) {
                this.discard();
                return;
            }else if (this.lifespan < 0) {
                BlockState bs = this.getInBlockState();
                FluidState fs = sl.getFluidState(blockPosition());
                // 不是空气 也不是流体
                if (!bs.isAir() && fs.is(Fluids.EMPTY)) {
                    this.discard();
                }
            }

            // 特效逻辑
            if (particle != null && this.random.nextInt(4) == 0) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double dist = this.random.nextDouble() * 6.0;
                double px = this.getX() + Math.cos(angle) * dist;
                double pz = this.getZ() + Math.sin(angle) * dist;

                for (int dy = 3; dy >= -3; dy--) {
                    BlockPos checkPos = BlockPos.containing(px, this.getY() + dy, pz);
                    FluidState fs = level().getFluidState(checkPos);

                    if (fs.getFluidType() == FluidRegister.LANTERN_BERRY_JUICE_TYPE.get()) {

                        double surfaceY = checkPos.getY() + fs.getHeight(level(), checkPos) + 0.1;

                        sl.sendParticles(
                                particle,
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
        if (tag.contains("DiffusateItemId")) {
            this.diffusateItemId = ResourceLocation.parse(tag.getString("DiffusateItemId"));
        }
        if (tag.contains("Lifespan")) {
            this.lifespan = tag.getInt("Lifespan");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.diffusateItemId != null) {
            tag.putString("DiffusateItemId", this.diffusateItemId.toString());
        }
        tag.putInt("Lifespan", this.lifespan);
    }
}