package com.thelema.thrive.generic.blockentity.modifier.cloud;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.generic.blockentity.modifier.Scattered;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.*;

public class SkyToCloud implements BiomeModifier {

    /** 启动门槛：BE 必须架到 200 以上。 */
    static final int MIN_Y = 200;

    /** 云团底层的水平半径。 */
    static final int CLUSTER_RADIUS = 2;
    /** 云团的垂直层数（逐层收窄）。 */
    static final int CLUSTER_LAYERS = 3;

    static final ResourceKey<Biome> TARGET = ResourceKey.create(
            Registries.BIOME, ResourceLocation.fromNamespaceAndPath("thrive", "cloud"));

    private static final BiomeTypeChanger ANY_TO_CLOUD = new BiomeTypeChanger(null, TARGET);
    private static final Scattered CLOUD = Scattered.of(64, 50, 64);

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        ServerLevel level = (ServerLevel) be.getLevel();
        if (be.getBlockPos().getY() < MIN_Y) return false;   // 启动门槛
        assert level != null;
        return level.getBiome(be.getBlockPos()).unwrapKey()
                .map(k -> k.location().getNamespace().equals("minecraft"))
                .orElse(false);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean rewrite = forBS(be, Ring.TICK_20, ANY_TO_CLOUD);
        boolean cloud = forPos(be, CLOUD, (b, p) -> buildCloud((ServerLevel) b.getLevel(), p));
        return rewrite & cloud;
    }

    static void buildCloud(ServerLevel level, BlockPos p) {
        if (!level.getBlockState(p).isAir()) return;   // 不判 y，不判 MAX_Y

        RandomSource random = level.random;
        BlockState cloud = BlockRegister.CLOUD.get().defaultBlockState();

        for (int dy = 0; dy < CLUSTER_LAYERS; dy++) {
            int r = CLUSTER_RADIUS - dy;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (random.nextInt(3) == 0) continue;
                    BlockPos q = p.offset(dx, dy, dz);
                    if (!level.getBlockState(q).isAir()) continue;
                    set(level, q, cloud);
                    markDirty(level, q);
                }
            }
        }
    }
}