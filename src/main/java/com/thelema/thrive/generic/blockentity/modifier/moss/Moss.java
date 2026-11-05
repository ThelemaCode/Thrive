package com.thelema.thrive.generic.blockentity.modifier.moss;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.PhasedModifier;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.BlockRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.markDirty;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.set;

/**
 * 任意群系 → Moss 扩张。
 * <p>
 * BE 在 moss 群系启动，Ring 范围内除已有 moss 区外全部改造。
 * REWRITE 在最后——前面阶段跑时群系还是原样，靠 {@link MossHelper#skipMoss} 跳过已有 moss。
 */
public class Moss extends PhasedModifier {

    private static final BiomeTypeChanger ANY_TO_MOSS =
            new BiomeTypeChanger(null, MossHelper.TARGET);

    public Moss() {
        addPos(Ring.TICK_20, MossHelper.skipMoss(Moss::prepare));      // PREPARE：清植被 + 改地表
        addPos(Ring.TICK_10, MossHelper.skipMoss(MossHelper::lake));   // LAKE
        addPos(Ring.TICK_20, MossHelper.skipMoss(MossHelper::deco));   // DECO
        addBS (Ring.TICK_20, ANY_TO_MOSS);                             // REWRITE
        addFinish(MossHelper.TREE_MOSS);                               // DONE
    }

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel().getBiome(be.getBlockPos()).is(MossHelper.TARGET);
    }

    private static void prepare(BiomeCoreBlockEntity be, BlockPos p) {
        clear(be, p);
        surface(be, p);
    }

    // ───── 清理（清树、花草、蕨、藤蔓） ─────

    static void clear(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockState s = level.getBlockState(p);
        if (s.isAir() || !s.getFluidState().isEmpty()) return;
        boolean plant = s.is(BlockTags.REPLACEABLE_BY_TREES)
                || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)
                || s.is(Blocks.MOSS_CARPET)
                || s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT)
                || s.is(BlockRegister.LANTERN_BERRY.get());
        boolean tree = s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES);
        if (!plant && !tree) return;
        set(level, p, Blocks.AIR.defaultBlockState());
        markDirty(level, p);
    }

    // ───── 地表（上方空气的自然地形 → 苔藓块） ─────

    static void surface(BiomeCoreBlockEntity be, BlockPos p) {
        ServerLevel level = (ServerLevel) be.getLevel();
        BlockState s = level.getBlockState(p);
        if (s.isAir() || !s.getFluidState().isEmpty()) return;
        if (!MossHelper.isNaturalTerrain(s)) return;
        if (!level.getBlockState(p.above()).isAir()) return;
        set(level, p, Blocks.MOSS_BLOCK.defaultBlockState());
        markDirty(level, p);
    }
}