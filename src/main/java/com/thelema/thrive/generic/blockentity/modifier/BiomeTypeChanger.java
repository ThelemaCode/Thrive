package com.thelema.thrive.generic.blockentity.modifier;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Set;
import java.util.function.Predicate;

/**
 * SectionApplier 的标准实现：把命中 unit 的群系改写为 target。
 * <p>
 * 源条件由 {@link Predicate} 表达。常用场景用静态工厂，复杂组合用 {@link #of}：
 * <ul>
 *     <li>{@link #of} 单群系 / 自定义 Predicate</li>
 *     <li>{@link #any} 任意群系</li>
 *     <li>{@link #fromTag} 单标签</li>
 *     <li>{@link #fromTags} 多标签并集（OR）</li>
 *     <li>{@link #fromTagExcluding} 标签差集（include 减去 exclude）</li>
 *     <li>{@link #fromVanilla} 原版命名空间</li>
 * </ul>
 * 目标自身永不被"源"捕获——即使源条件匹配 target，也不会重复处理。
 */
public final class BiomeTypeChanger implements SectionApplier {

    private final Predicate<Holder<Biome>> sources;
    private final ResourceKey<Biome> target;

    private BiomeTypeChanger(Predicate<Holder<Biome>> sources, ResourceKey<Biome> target) {
        this.sources = sources;
        this.target = target;
    }

    // ───── 工厂 ─────

    /** 单群系。 */
    public static BiomeTypeChanger of(ResourceKey<Biome> source, ResourceKey<Biome> target) {
        return new BiomeTypeChanger(b -> b.is(source), target);
    }

    /** 自定义 Predicate，用于任意复杂组合。 */
    public static BiomeTypeChanger of(Predicate<Holder<Biome>> sources, ResourceKey<Biome> target) {
        return new BiomeTypeChanger(sources, target);
    }

    /** 任意群系。 */
    public static BiomeTypeChanger any(ResourceKey<Biome> target) {
        return new BiomeTypeChanger(b -> true, target);
    }

    /** 单标签。 */
    public static BiomeTypeChanger fromTag(TagKey<Biome> tag, ResourceKey<Biome> target) {
        return new BiomeTypeChanger(b -> b.is(tag), target);
    }

    /** 多标签并集：属于任一标签即匹配。 */
    @SafeVarargs
    public static BiomeTypeChanger fromTags(ResourceKey<Biome> target, TagKey<Biome>... tags) {
        return new BiomeTypeChanger(b -> {
            for (TagKey<Biome> tag : tags)
                if (b.is(tag)) return true;
            return false;
        }, target);
    }

    /** 标签差集：属于 include，且不属于任一 exclude。 */
    @SafeVarargs
    public static BiomeTypeChanger fromTagExcluding(ResourceKey<Biome> target,
                                                    TagKey<Biome> include,
                                                    TagKey<Biome>... exclude) {
        return new BiomeTypeChanger(b -> {
            if (!b.is(include)) return false;
            for (TagKey<Biome> tag : exclude)
                if (b.is(tag)) return false;
            return true;
        }, target);
    }

    /** 原版群系（namespace == minecraft）。 */
    public static BiomeTypeChanger fromVanilla(ResourceKey<Biome> target) {
        return new BiomeTypeChanger(
                b -> b.unwrapKey()
                        .map(k -> k.location().getNamespace().equals("minecraft"))
                        .orElse(false),
                target);
    }

    // ───── 应用 ─────

    @Override
    public void apply(ChunkAccess chunk, int sectionIndex, Set<Integer> units) {
        if (!(chunk instanceof LevelChunk lc)) return;
        ServerLevel level = (ServerLevel) lc.getLevel();
        Holder<Biome> t = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(target);
        BiomeUtil.applyBiomeSection(chunk, sectionIndex, units, t, prev ->
                !prev.unwrapKey().map(k -> k.equals(target)).orElse(false)
                        && sources.test(prev));
    }
}