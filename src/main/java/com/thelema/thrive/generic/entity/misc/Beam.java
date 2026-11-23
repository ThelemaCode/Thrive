package com.thelema.thrive.generic.entity.misc;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.register.EntityRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Beam extends Entity {

    /**常量*/
    public static final String FIRE = ThriveMod.MOD_ID + ":fire";

    /**同步属性*/
    public static final EntityDataAccessor<Vector3f> START = SynchedEntityData.defineId(Beam.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<Vector3f> END = SynchedEntityData.defineId(Beam.class, EntityDataSerializers.VECTOR3);
    public static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(Beam.class, EntityDataSerializers.STRING);

    /**生命周期*/
    public int lifespan = 200;

    public Beam(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * 直接在世界中生成一个Beam
     */
    public static void spawn(ServerLevel level, Vec3 start, Vec3 end, String type) {
        Beam beam = new Beam(EntityRegister.BEAM.get(), level);

        beam.entityData.set(START, new Vector3f((float) start.x, (float) start.y, (float) start.z));
        beam.entityData.set(END, new Vector3f((float) end.x, (float) end.y, (float) end.z));
        beam.entityData.set(TYPE, type);
        beam.moveTo(start);
        level.addFreshEntity(beam);
    }

    @Override
    public void tick() {
        super.tick();
        lifespan--;
        if (lifespan <= 0){
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START, new Vector3f(0.0F, 0.0F, 0.0F))
                .define(END, new Vector3f(0.0F, 0.0F, 0.0F))
                .define(TYPE, "null")
                .build();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(START, new Vector3f(
                tag.getFloat("sx"),
                tag.getFloat("sy"),
                tag.getFloat("sz")
        ));
        entityData.set(END, new Vector3f(
                tag.getFloat("ex"),
                tag.getFloat("ey"),
                tag.getFloat("ez")
        ));
        entityData.set(TYPE, tag.getString("type"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        Vector3f s = entityData.get(START);
        tag.putFloat("sx", s.x);
        tag.putFloat("sy", s.y);
        tag.putFloat("sz", s.z);
        Vector3f e = entityData.get(END);
        tag.putFloat("ex", e.x);
        tag.putFloat("ey", e.y);
        tag.putFloat("ez", e.z);
        String t = entityData.get(TYPE);
        tag.putString("type", t);
    }
}
