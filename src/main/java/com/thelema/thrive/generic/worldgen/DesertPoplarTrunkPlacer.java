package com.thelema.thrive.generic.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thelema.thrive.register.TrunkPlacerRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * 胡杨的树干：主干净直长到 {@code fork_height}，然后**分叉**成 2~4 根主干向外倾斜生长，
 * 每根主干的侧面还会随机长小枝，枝头挂树叶。
 *
 * <p>原版没有这种形状的树干生成器（最接近的 {@code upward_branching_trunk_placer} 是
 * 从顶部分叉、不开小枝），所以必须自己写一个 {@link TrunkPlacer} 并注册
 * {@link TrunkPlacerType}，否则 JSON 里引用不到这个类型，注册表加载直接失败。
 *
 * <p>对应 JSON：{@code data/thrive/worldgen/configured_feature/desert_poplar.json}
 * 里的 {@code "trunk_placer": { "type": "thrive:desert_poplar_trunk_placer", ... }}。
 */
public class DesertPoplarTrunkPlacer extends TrunkPlacer {

    public static final MapCodec<DesertPoplarTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.intRange(0, 32).fieldOf("base_height").forGetter(p -> p.baseHeight),
                    Codec.intRange(0, 24).fieldOf("height_rand_a").forGetter(p -> p.heightRandA),
                    Codec.intRange(0, 24).fieldOf("height_rand_b").forGetter(p -> p.heightRandB),
                    Codec.intRange(1, 16).fieldOf("fork_height").forGetter(p -> p.forkHeight),
                    Codec.intRange(1, 4).fieldOf("min_branches").forGetter(p -> p.minBranches),
                    Codec.intRange(1, 4).fieldOf("max_branches").forGetter(p -> p.maxBranches),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("branch_chance").forGetter(p -> p.branchChance),
                    Codec.intRange(1, 8).fieldOf("branch_length").forGetter(p -> p.branchLength)
            ).apply(instance, DesertPoplarTrunkPlacer::new));

    private final int forkHeight;
    private final int minBranches;
    private final int maxBranches;
    private final float branchChance;
    private final int branchLength;

    public DesertPoplarTrunkPlacer(int baseHeight, int heightRandA, int heightRandB,
                                   int forkHeight, int minBranches, int maxBranches,
                                   float branchChance, int branchLength) {
        super(baseHeight, heightRandA, heightRandB);
        this.forkHeight = forkHeight;
        this.minBranches = minBranches;
        this.maxBranches = maxBranches;
        this.branchChance = branchChance;
        this.branchLength = branchLength;
    }

    @Override
    protected @NotNull TrunkPlacerType<?> type() {
        return TrunkPlacerRegister.DESERT_POPLAR_TRUNK_PLACER.get();
    }

    @Override
    public @NotNull List<FoliagePlacer.FoliageAttachment> placeTrunk(
            LevelSimulatedReader level,
            BiConsumer<BlockPos, BlockState> setter,
            RandomSource random,
            int freeTreeHeight,
            BlockPos startPos,
            TreeConfiguration config) {

        List<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<>();

        // 1. 主干：直着长到分叉点
        BlockPos pos = startPos;
        for (int h = 0; h < this.forkHeight && h < freeTreeHeight; h++) {
            placeLog(level, setter, random, pos, config, Direction.Axis.Y);
            pos = pos.above();
        }

        // 2. 分叉：水平四个方向里随机挑几个，各放一格原木
        List<Direction> directions = new ArrayList<>(List.of(
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        shuffle(directions, random);

        int count = Math.min(this.minBranches + random.nextInt(this.maxBranches - this.minBranches + 1),
                directions.size());
        int remaining = freeTreeHeight - this.forkHeight;

        for (int i = 0; i < count; i++) {
            Direction dir = directions.get(i);
            BlockPos forkPos = pos.relative(dir);
            placeLog(level, setter, random, forkPos, config, dir.getAxis());
            // 3. 每根分叉继续往上长，并随机开小枝
            growFork(level, setter, random, forkPos, dir, remaining, config, attachments);
        }

        return attachments;
    }

    /** 从分叉点向上长一根主干，路上可能横移一格、可能长小枝。 */
    private void growFork(LevelSimulatedReader level,
                          BiConsumer<BlockPos, BlockState> setter,
                          RandomSource random,
                          BlockPos startPos,
                          Direction growthDirection,
                          int remainingHeight,
                          TreeConfiguration config,
                          List<FoliagePlacer.FoliageAttachment> attachments) {

        BlockPos pos = startPos;

        for (int h = 0; h < remainingHeight; h++) {
            placeLog(level, setter, random, pos, config, Direction.Axis.Y);

            // 第 2/3 格有概率向外挪一格，做出"张开"的姿态
            if ((h == 1 || h == 2) && random.nextFloat() < 0.7F) {
                pos = pos.relative(growthDirection);
                placeLog(level, setter, random, pos, config, growthDirection.getAxis());
            }

            if (h >= 1 && h <= remainingHeight - 2 && random.nextFloat() < this.branchChance) {
                growTwig(level, setter, random, pos, config, attachments);
            }

            pos = pos.above();
        }

        // 枝头的树叶挂点
        attachments.add(new FoliagePlacer.FoliageAttachment(pos.above(2), 0, false));
    }

    /** 一根斜向上的小枝，末梢挂树叶。 */
    private void growTwig(LevelSimulatedReader level,
                          BiConsumer<BlockPos, BlockState> setter,
                          RandomSource random,
                          BlockPos startPos,
                          TreeConfiguration config,
                          List<FoliagePlacer.FoliageAttachment> attachments) {

        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos pos = startPos;

        for (int i = 0; i < this.branchLength; i++) {
            pos = pos.relative(dir);
            if (random.nextBoolean()) {
                pos = pos.above();
            }
            placeLog(level, setter, random, pos, config, dir.getAxis());

            if (i == this.branchLength - 1) {
                attachments.add(new FoliagePlacer.FoliageAttachment(pos.above(2), 0, false));
            }
        }
    }

    /**
     * 放一格原木，并把它转到 {@code axis} 方向。
     *
     * <p>只是给原版 {@code placeLog(..., Function&lt;BlockState, BlockState&gt;)} 套一层：
     * 原版已经做了「位置合法才放」的检查，比自己 {@code setter.accept} 安全。
     */
    private void placeLog(LevelSimulatedReader level,
                          BiConsumer<BlockPos, BlockState> setter,
                          RandomSource random,
                          BlockPos pos,
                          TreeConfiguration config,
                          Direction.Axis axis) {
        placeLog(level, setter, random, pos, config,
                state -> state.setValue(RotatedPillarBlock.AXIS, axis));
    }

    private static void shuffle(List<Direction> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Direction tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
