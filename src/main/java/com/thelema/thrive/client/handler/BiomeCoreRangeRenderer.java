package com.thelema.thrive.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeUtil;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = ThriveMod.MOD_ID, value = Dist.CLIENT)
public class BiomeCoreRangeRenderer {

    private static final float[][] RAINBOW = {
            {1.0f, 0.0f, 0.0f}, {1.0f, 0.5f, 0.0f}, {1.0f, 1.0f, 0.0f},
            {0.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 1.0f}, {0.0f, 0.0f, 1.0f},
            {0.5f, 0.0f, 1.0f},
    };

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;
        if (!isHoldingCore(player)) return;

        PoseStack ms = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        ms.pushPose();
        ms.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());

        int rd = mc.options.renderDistance().get();
        ChunkPos pc = player.chunkPosition();
        for (int dx = -rd; dx <= rd; dx++)
            for (int dz = -rd; dz <= rd; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(pc.x + dx, pc.z + dz, false);
                if (chunk == null) continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof BiomeCoreBlockEntity core)) continue;
                    drawCube(ms, vc, core);
                }
            }

        buffers.endBatch(RenderType.debugQuads());
        ms.popPose();
    }

    private static boolean isHoldingCore(LocalPlayer player) {
        var core = BlockRegister.BIOME_CORE.get().asItem();
        return player.getMainHandItem().is(core) || player.getOffhandItem().is(core);
    }

    private static float[] colorFor(BlockPos base) {
        int cx = base.getX() >> 4;
        int cz = base.getZ() >> 4;
        return RAINBOW[Math.floorMod(cx * 31 + cz * 7, 7)];
    }

    private static void drawCube(PoseStack ms, VertexConsumer vc, BiomeCoreBlockEntity core) {
        BlockPos base = core.getBlockPos();
        int cr = core.chunkRadius;
        float[] c = colorFor(base);

        float minX = BiomeUtil.minX(base, cr);
        float maxX = BiomeUtil.maxX(base, cr) + 1;
        float minZ = BiomeUtil.minZ(base, cr);
        float maxZ = BiomeUtil.maxZ(base, cr) + 1;
        float minY = BiomeUtil.minY(base, cr);
        float maxY = BiomeUtil.maxY(base, cr) + 1;

        // 6 个面，每面 4 条边框，共 24 个矩形面
        // axis=0 (x 平面): u=z, v=y
        faceBorders(ms, vc, c, 0, minX, minZ, maxZ, minY, maxY);   // 左面
        faceBorders(ms, vc, c, 0, maxX, minZ, maxZ, minY, maxY);   // 右面
        // axis=1 (y 平面): u=x, v=z
        faceBorders(ms, vc, c, 1, minY, minX, maxX, minZ, maxZ);   // 底面
        faceBorders(ms, vc, c, 1, maxY, minX, maxX, minZ, maxZ);   // 顶面
        // axis=2 (z 平面): u=x, v=y
        faceBorders(ms, vc, c, 2, minZ, minX, maxX, minY, maxY);   // 前面
        faceBorders(ms, vc, c, 2, maxZ, minX, maxX, minY, maxY);   // 后面
    }

    /**
     * 在一个轴对齐平面（axis 轴，位置 coord）上画 4 条 1 格宽边框矩形。
     * 每条边框在平面内的长轴内缩 0.5 变 (n-1)，避 z-fighting。
     */
    private static void faceBorders(PoseStack ms, VertexConsumer vc, float[] c,
                                    int axis, float coord,
                                    float uMin, float uMax, float vMin, float vMax) {
        float u0 = uMin + 0.5f, u1 = uMax - 0.5f;
        float v0 = vMin + 0.5f, v1 = vMax - 0.5f;

        // u = uMin 边（宽 1，沿 v 延伸 v0..v1）
        quad(ms, vc, c, axis, coord, uMin, v0, uMin + 1, v0, uMin + 1, v1, uMin, v1);
        // u = uMax 边
        quad(ms, vc, c, axis, coord, uMax - 1, v0, uMax, v0, uMax, v1, uMax - 1, v1);
        // v = vMin 边（宽 1，沿 u 延伸 u0..u1）
        quad(ms, vc, c, axis, coord, u0, vMin, u1, vMin, u1, vMin + 1, u0, vMin + 1);
        // v = vMax 边
        quad(ms, vc, c, axis, coord, u0, vMax - 1, u1, vMax - 1, u1, vMax, u0, vMax);
    }

    /** 在 (axis, coord) 平面上，用平面内 (u, v) 坐标画一个四边形。 */
    private static void quad(PoseStack ms, VertexConsumer vc, float[] c,
                             int axis, float coord,
                             float u1, float v1, float u2, float v2,
                             float u3, float v3, float u4, float v4) {
        PoseStack.Pose pose = ms.last();
        vertex(pose, vc, c, axis, coord, u1, v1);
        vertex(pose, vc, c, axis, coord, u2, v2);
        vertex(pose, vc, c, axis, coord, u3, v3);
        vertex(pose, vc, c, axis, coord, u4, v4);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, float[] c,
                               int axis, float coord, float u, float v) {
        float x, y, z;
        switch (axis) {
            case 0 -> { x = coord; z = u; y = v; }
            case 1 -> { y = coord; x = u; z = v; }
            default -> { z = coord; x = u; y = v; }
        }
        vc.addVertex(pose, x, y, z).setColor(c[0], c[1], c[2], 1.0f);
    }
}