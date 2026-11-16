package com.thelema.thrive.generic.blockentity.modifier.b.moss;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import com.thelema.thrive.generic.blockentity.modifier.BiomeModifier;
import com.thelema.thrive.generic.blockentity.modifier.BiomeTypeChanger;
import com.thelema.thrive.generic.blockentity.modifier.Ring;
import com.thelema.thrive.register.BlockRegister;
import com.thelema.thrive.register.constant.BiomeConstant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.*;

public class Moss implements BiomeModifier {

    private static final BiomeTypeChanger ANY_TO_MOSS = BiomeTypeChanger.any(BiomeConstant.MOSS);

    /**
     * 流水线延迟：每个阶段落后上一阶段几环。
     */
    private static final int DELAY = 2;

    private static final Ring R_PREPARE = new Ring(20);
    private static final Ring R_LAKE = new Ring(10, 0, 0, DELAY);
    private static final Ring R_DECO = new Ring(20, 0, 0, DELAY);
    private static final Ring R_REWRITE = new Ring(20, 0, 0, DELAY * 2);

    @Override
    public boolean canUse(BiomeCoreBlockEntity be) {
        return be.getLevel() instanceof ServerLevel sl && sl.getBiome(be.getBlockPos()).is(BiomeConstant.MOSS);
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        boolean prepare = forPos(be, R_PREPARE, MossHelper.skipMoss(Moss::prepare));
        boolean lake = forPos(be, R_LAKE, MossHelper.skipMoss(MossHelper::lake));
        boolean deco = forPos(be, R_DECO, MossHelper.skipMoss(MossHelper::deco));
        boolean rewrite = forBS(be, R_REWRITE, ANY_TO_MOSS);

        return prepare & lake & deco & rewrite;
    }

    // ───── PREPARE：清植被 + 铺 MOSS_BLOCK ─────

    private static void prepare(BiomeCoreBlockEntity be, BlockPos p) {
        clear(be, p);
        surface(be, p);
    }

    // ───── 清理（清树、花草、蕨、藤蔓） ─────

    static void clear(BiomeCoreBlockEntity be, BlockPos p) {
        if (be.getLevel() instanceof ServerLevel sl) {
            BlockState s = sl.getBlockState(p);
            if (s.isAir() || !s.getFluidState().isEmpty()) return;
            boolean plant = s.is(BlockTags.REPLACEABLE_BY_TREES)
                    || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)
                    || s.is(Blocks.MOSS_CARPET)
                    || s.is(Blocks.CAVE_VINES) || s.is(Blocks.CAVE_VINES_PLANT)
                    || s.is(BlockRegister.LANTERN_BERRY.get());
            boolean tree = s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES);
            if (!plant && !tree) return;
            set(sl, p, Blocks.AIR.defaultBlockState());
            markDirty(sl, p);
        }

    }

    // ───── 地表（上方空气的自然地形 → 苔藓块） ─────

    static void surface(BiomeCoreBlockEntity be, BlockPos p) {
        if (be.getLevel() instanceof ServerLevel sl) {
            BlockState s = sl.getBlockState(p);
            if (s.isAir() || !s.getFluidState().isEmpty()) return;
            if (!MossHelper.isNaturalTerrain(s)) return;
            if (!sl.getBlockState(p.above()).isAir()) return;
            set(sl, p, Blocks.MOSS_BLOCK.defaultBlockState());
            markDirty(sl, p);
        }
    }
}