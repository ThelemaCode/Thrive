package com.thelema.thrive.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thelema.thrive.generic.entity.misc.Beam;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// 待完善
public class BeamRenderer extends EntityRenderer<Beam> {

    public BeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0F;
    }

    @Override
    public void render(Beam beam, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        SynchedEntityData data = beam.getEntityData();
        Vector3f startWorld = data.get(Beam.START);
        Vector3f endWorld = data.get(Beam.END);
        String type = data.get(Beam.TYPE);

        if (Beam.FIRE.equals(type)) {
            poseStack.pushPose();

            Vector3f S_local = new Vector3f(
                    startWorld.x() - (float) beam.getX(),
                    startWorld.y() - (float) beam.getY(),
                    startWorld.z() - (float) beam.getZ()
            );
            Vector3f E_local = new Vector3f(
                    endWorld.x() - (float) beam.getX(),
                    endWorld.y() - (float) beam.getY(),
                    endWorld.z() - (float) beam.getZ()
            );

            float totalTicks = beam.tickCount + partialTick;

            float outerFactor = 0F;
            float middleFactor = 0F;
            float innerFactor = 0F;
            float disappearAlpha = 1F;

            final float T_OUTER_MAX = 8F;
            final float T_MID_MAX = 16F;
            final float T_INNER_MAX = 30F;
            final float T_KEEP_END = 50F;

            if (totalTicks <= T_OUTER_MAX) {
                outerFactor = easeOutCubic(totalTicks / T_OUTER_MAX);
            } else if (totalTicks <= T_MID_MAX) {
                outerFactor = 1F;
                middleFactor = easeOutCubic((totalTicks - T_OUTER_MAX) / (T_MID_MAX - T_OUTER_MAX));
            } else if (totalTicks <= T_INNER_MAX) {
                outerFactor = 1F;
                middleFactor = 1F;
                innerFactor = easeOutCubic((totalTicks - T_MID_MAX) / (T_INNER_MAX - T_MID_MAX));
            } else if (totalTicks <= T_KEEP_END) {
                outerFactor = 1F;
                middleFactor = 1F;
                innerFactor = 1F;
                disappearAlpha = 1F;
            } else {
                float shrinkProgress = Math.min(1F, (totalTicks - T_KEEP_END) / 40F);
                float shrinkFactor = 1F - easeInQuad(shrinkProgress);

                float minCore = 0.02F;
                outerFactor = Math.max(minCore * 0.8F, 1F * shrinkFactor);
                middleFactor = Math.max(minCore * 0.6F, 1F * shrinkFactor);
                innerFactor = Math.max(minCore, 1F * shrinkFactor);

                disappearAlpha = 1F - shrinkProgress * 0.3F;
            }

            VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
            Matrix4f mat = poseStack.last().pose();

            float baseThickness = 0.35F;

            // 内层白光：小而亮，只占核心
            float innerThick = baseThickness * innerFactor * 0.35F;
            drawBeamQuads(mat, consumer, beam, S_local, E_local, innerThick,
                    1F, 1F, 1F, 1F * disappearAlpha);

            // 中层红光：压在白光外面
            float middleThick = baseThickness * middleFactor * 0.7F;
            drawBeamQuads(mat, consumer, beam, S_local, E_local, middleThick,
                    1F, 0.25F, 0.25F, 0.55F * disappearAlpha);

            // 外层红光：最后画，最粗，负责体积感
            float outerThick = baseThickness * outerFactor;
            drawBeamQuads(mat, consumer, beam, S_local, E_local, outerThick,
                    1F, 0.3F, 0.3F, 0.3F * disappearAlpha);

            poseStack.popPose();
        }

        super.render(beam, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void drawBeamQuads(Matrix4f mat, VertexConsumer vc, Beam beam,
                                      Vector3f S, Vector3f E, float thick,
                                      float r, float g, float b, float a) {
        Vector3f dir = new Vector3f(E).sub(S);
        float len2 = dir.lengthSquared();
        if (len2 < 1e-6f) return;

        Vector3f temp = Math.abs(dir.y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f side1 = new Vector3f(temp).cross(dir).normalize();
        Vector3f side2 = new Vector3f(dir).cross(side1).normalize();

        Vector3f o1 = new Vector3f(side1).mul(thick);
        Vector3f o2 = new Vector3f(side2).mul(thick);

        long id = beam.getId();

        Vector3f s1 = jitter(new Vector3f(S).add(o1).add(o2), thick * 0.3F, id * 31L + 1L);
        Vector3f s2 = jitter(new Vector3f(S).sub(o1).add(o2), thick * 0.3F, id * 31L + 2L);
        Vector3f s3 = jitter(new Vector3f(S).sub(o1).sub(o2), thick * 0.3F, id * 31L + 3L);
        Vector3f s4 = jitter(new Vector3f(S).add(o1).sub(o2), thick * 0.3F, id * 31L + 4L);

        Vector3f e1 = jitter(new Vector3f(E).add(o1).add(o2), thick * 0.4F, id * 31L + 5L);
        Vector3f e2 = jitter(new Vector3f(E).sub(o1).add(o2), thick * 0.4F, id * 31L + 6L);
        Vector3f e3 = jitter(new Vector3f(E).sub(o1).sub(o2), thick * 0.4F, id * 31L + 7L);
        Vector3f e4 = jitter(new Vector3f(E).add(o1).sub(o2), thick * 0.4F, id * 31L + 8L);

        addQuad(vc, mat, s1, e1, e2, s2, r, g, b, a);
        addQuad(vc, mat, s2, e2, e3, s3, r, g, b, a);
        addQuad(vc, mat, s3, e3, e4, s4, r, g, b, a);
        addQuad(vc, mat, s4, e4, e1, s1, r, g, b, a);
    }

    private static Vector3f jitter(Vector3f v, float amount, long seed) {
        long s = seed * 9301L + 49297L;
        float jx = ((float) (s % 1000000L) / 500000F - 1F) * amount;
        float jy = ((float) ((s >> 3) % 1000000L) / 500000F - 1F) * amount;
        float jz = ((float) ((s >> 6) % 1000000L) / 500000F - 1F) * amount;
        return new Vector3f(v.x + jx, v.y + jy, v.z);
    }

    private static float easeOutCubic(float t) {
        t = Math.max(0F, Math.min(1F, t));
        return 1F - (1F - t) * (1F - t) * (1F - t);
    }

    private static float easeInQuad(float t) {
        t = Math.max(0F, Math.min(1F, t));
        return t * t;
    }

    private static void addQuad(VertexConsumer vc, Matrix4f mat,
                                Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3,
                                float r, float g, float b, float a) {
        vc.addVertex(mat, v0.x, v0.y, v0.z).setColor(r, g, b, a);
        vc.addVertex(mat, v1.x, v1.y, v1.z).setColor(r, g, b, a);
        vc.addVertex(mat, v2.x, v2.y, v2.z).setColor(r, g, b, a);
        vc.addVertex(mat, v3.x, v3.y, v3.z).setColor(r, g, b, a);
    }

    @Override
    public @Nullable ResourceLocation getTextureLocation(Beam beam) {
        return null;
    }
}
