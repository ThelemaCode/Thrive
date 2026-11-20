package com.thelema.thrive.client.render.entity;

import com.mojang.datafixers.util.Pair;
import com.thelema.thrive.ThriveMod;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * 胡杨木船的渲染器。
 *
 * <p>船体几何在所有木质类型之间完全相同，只有贴图不同，所以直接复用原版的
 * {@code OAK_BOAT} / {@code OAK_CHEST_BOAT} 模型层——不用自己注册
 * {@code ModelLayerLocation}，也不用在 {@code RegisterLayerDefinitions} 里加东西。
 */
@OnlyIn(Dist.CLIENT)
public class ThriveBoatRenderer extends BoatRenderer {

    private final Pair<ResourceLocation, ListModel<Boat>> resource;

    public ThriveBoatRenderer(EntityRendererProvider.Context context, boolean chest) {
        super(context, chest);
        // 直接借用原版橡木船的模型层：船体几何在所有木质类型之间完全一样，
        // 只有贴图不同。这样不用自己注册 ModelLayerLocation。
        ModelPart part = context.bakeLayer(chest
                ? ModelLayers.createChestBoatModelName(Boat.Type.OAK)
                : ModelLayers.createBoatModelName(Boat.Type.OAK));
        ListModel<Boat> model = chest ? new ChestBoatModel(part) : new BoatModel(part);
        this.resource = Pair.of(
                ResourceLocation.fromNamespaceAndPath(ThriveMod.MOD_ID,
                        "textures/entity/" + (chest ? "chest_boat" : "boat") + "/desert_poplar.png"),
                model);
    }

    @Override
    public @NotNull Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(@NotNull Boat boat) {
        return this.resource;
    }
}
