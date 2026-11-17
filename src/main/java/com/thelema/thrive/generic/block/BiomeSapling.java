package com.thelema.thrive.generic.block;

import com.thelema.thrive.generic.structure.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 按「树苗所在群系」决定长哪棵树的树苗。
 *
 * <p>结构与群系的对应关系在 {@link #MAP} 里，带保底（{@link #FALLBACK_TREE}）：
 * 没登记的群系（包括其它 mod 的群系）也能长出一棵树，不会变成「种下去没反应」。
 *
 * <p>锚点从模板里的 {@code biome_core} 方块读，所以 NBT 里必须放一块；
 * 改树形只改 NBT，不用动代码。
 */
public class BiomeSapling extends StructureSapling {

    /** 保底结构：未注册的群系（如 mod 群系）也至少能长出一棵树。 */
    public static final ResourceLocation FALLBACK_TREE =
            ResourceLocation.fromNamespaceAndPath("thrive", "biome_core_tree_plains");

    public static final Map<ResourceLocation, ResourceLocation> MAP = new HashMap<>();

    public static void register(String id, String loc) {
        MAP.put(ResourceLocation.parse(id), ResourceLocation.parse(loc));
    }

    static {
        register("minecraft:forest", "thrive:biome_core_tree_forest");
        register("minecraft:plains", "thrive:biome_core_tree_plains");
        register("thrive:garden", "thrive:biome_core_tree_garden");
        register("thrive:moss", "thrive:biome_core_tree_moss");
    }

    public static ResourceLocation treeStructureFor(Holder<Biome> biome) {
        // unwrapKey 为空（动态群系）或未登记时，Optional.map 里返回 null 会变成 empty，落到保底
        return biome.unwrapKey()
                .map(key -> MAP.get(key.location()))
                .orElse(FALLBACK_TREE);
    }

    public BiomeSapling() {
        super();
    }

    @Nullable
    @Override
    protected Placement resolve(ServerLevel level, BlockPos pos, RandomSource random) {
        StructureTemplate template = level.getServer().getStructureManager()
                .get(treeStructureFor(level.getBiome(pos)))
                .orElse(null);
        if (template == null) return null;

        BlockPos localAnchor = StructurePlacer.findCoreMarker(template);
        if (localAnchor == null) return null;

        return new Placement(template, localAnchor, Rotation.getRandom(random));
    }
}
