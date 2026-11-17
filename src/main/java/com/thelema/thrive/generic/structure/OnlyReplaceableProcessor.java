package com.thelema.thrive.generic.structure;

import com.thelema.thrive.register.ProcessorRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

/**
 * 只允许「覆盖可替换方块」的处理器。
 *
 * <p>{@code StructureTemplate.placeInWorld} 是**无条件** {@code setBlock} 的：
 * 模板里有什么就往世界上写什么，世界原本是泥土、石头还是基岩都照写不误。
 * 这就是树苗长出来把地面刮掉一层、把基岩挖掉的原因。
 *
 * <p>这个处理器把「写」变成「有条件地写」：目标格不是空气/花草/水/树叶之类的可替换方块时，
 * 直接返回 {@code null}，这一块就不放了（和 jigsaw 放结构的思路一样，只是判据更严）。
 *
 * <p>判断依据是**世界当前状态**，不是模板状态——模板里是泥土、世界是草地时，
 * 我们要保住草地，所以看世界。
 */
public class OnlyReplaceableProcessor extends StructureProcessor {

    public static final OnlyReplaceableProcessor INSTANCE = new OnlyReplaceableProcessor();

    public static final MapCodec<OnlyReplaceableProcessor> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public StructureTemplate.StructureBlockInfo process(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo original,
            StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings,
            @Nullable StructureTemplate template) {
        // 注意：这一格的世界坐标是 current.pos()。
        // 形参 pos 是「结构锚点」（placeInWorld 的第三个参数），不是这一格的位置，别用错。
        return isReplaceable(level.getBlockState(current.pos())) ? current : null;
    }

    /**
     * 这个方块能不能被结构覆盖。
     *
     * <p>只放行「空气 + 植物 + 水」这一层；泥土、石头、原木、木板、基岩、箱子……
     * 一律视为不可覆盖，结构遇到就跳过。
     */
    public static boolean isReplaceable(BlockState state) {
        return state.isAir()                              // 空气 / 洞穴空气 / 虚空空气
                || state.is(BlockTags.REPLACEABLE)            // 短草、蕨、雪、水、藤、火、结构空位…
                || state.is(BlockTags.REPLACEABLE_BY_TREES)   // 树叶、高草、大型花、海草…
                || state.is(BlockTags.FLOWERS)                // 小花（原版树不覆盖，但我们希望花被树顶掉）
                || state.is(BlockTags.SAPLINGS);              // 别把树苗留在树干里
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ProcessorRegister.ONLY_REPLACEABLE.get();
    }
}
