package com.thelema.thrive.client.render.blockentity.cap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thelema.thrive.generic.blockentity.cap.Rotator;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RotatorRenderer {

    private static final float SCALE = 0.6f;

    public static void render(Rotator rotator, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay,
                              ItemRenderer itemRenderer) {
        if (rotator.isEmpty()) return;

        Level level = rotator.owner.getLevel();
        int renderLight = 0xF000F0; // 全亮，防止黑块

        double time = (level != null ? level.getGameTime() : 0) + partialTicks;

        // ====== 公转（环绕）计算 ======
        float angleX = (float)(time * rotator.crx / 20.0) % 360f;
        float angleY = (float)(time * rotator.cry / 20.0) % 360f;
        float angleZ = (float)(time * rotator.crz / 20.0) % 360f;

        Quaternionf orbitQuat = new Quaternionf().rotationXYZ(
                (float)Math.toRadians(angleX),
                (float)Math.toRadians(angleY),
                (float)Math.toRadians(angleZ)
        );

        int count = rotator.items.size();

        for (int i = 0; i < count; i++) {
            ItemStack stack = rotator.items.get(i);

            // ====== 计算环绕位置 ======
            double theta = Math.toRadians((360.0 / count) * i);
            float baseX = (float)(Math.cos(theta) * rotator.radiusX);
            float baseY = 0;
            float baseZ = (float)(Math.sin(theta) * rotator.radiusZ);

            Vector3f orbitPos = new Vector3f(baseX, baseY, baseZ).rotate(orbitQuat);

            ms.pushPose();

            // 1. 移动到最终位置
            ms.translate(0.5 + rotator.ox + orbitPos.x,
                    0.5 + rotator.oy + orbitPos.y,
                    0.5 + rotator.oz + orbitPos.z);

            // ====== 修复抽搐 & 增强旋转 ======
            // 2. 使用 double 进行取余运算，彻底解决长时间运行后的浮点数精度丢失（抽搐的主因）
            double itemTime = time + i * 15.0; // 给每个物品错开时间
            double spinX = (itemTime * rotator.rx / 20.0) % 360.0;
            double spinY = (itemTime * rotator.ry / 20.0) % 360.0;
            double spinZ = (itemTime * rotator.rz / 20.0) % 360.0;

            // 3. 使用独立四元数相乘，突破欧拉角死锁，实现平滑的多轴翻滚
            Quaternionf spinQuat = new Quaternionf()
                    .rotationY((float) Math.toRadians(spinY)) // 先绕 Y 轴自转
                    .rotationX((float) Math.toRadians(spinX)) // 再绕 X 轴翻滚
                    .rotationZ((float) Math.toRadians(spinZ));// 最后绕 Z 轴侧倾
            ms.mulPose(spinQuat);

            // 4. 缩放
            ms.scale(SCALE, SCALE, SCALE);

            // 5. 渲染
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, renderLight, overlay, ms, buffer, level, 0);

            ms.popPose();
        }
    }
}