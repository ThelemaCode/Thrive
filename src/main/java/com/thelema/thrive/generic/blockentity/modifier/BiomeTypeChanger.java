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
 * 源条件由 {@link Predicate} 表达，四种常用场景用静态工厂构造：
 * {@link #of} 指定群系、{@link #any} 任意群系、{@link #fromTag} 标签、
 * {@link #fromVanilla} 原版命名空间。
 * <p>
 * 目标自身永不被"源"捕获——即使源条件匹配 target，也不会重复处理。
 * 高频路径上建 {@code static final} 单例，不要每 tick new。
 */
public final class BiomeTypeChanger implements SectionApplier {

    private final Predicate<Holder<Biome>> sources;
    private final ResourceKey<Biome> target;

    private BiomeTypeChanger(Predicate<Holder<Biome>> sources, ResourceKey<Biome> target) {
        this.sources = sources;
        this.target = target;
    }

    /** 源 = 指定群系。 */
    public static BiomeTypeChanger of(ResourceKey<Biome> source, ResourceKey<Biome> target) {
        return new BiomeTypeChanger(b -> b.is(source), target);
    }

    /** 源 = 任意群系（旧 null 语义）。 */
    public static BiomeTypeChanger any(ResourceKey<Biome> target) {
        return new BiomeTypeChanger(b -> true, target);
    }

    /** 源 = 标签内所有群系。 */
    public static BiomeTypeChanger fromTag(TagKey<Biome> tag, ResourceKey<Biome> target) {
        return new BiomeTypeChanger(b -> b.is(tag), target);
    }

    /** 源 = 原版群系（namespace == minecraft）。 */
    public static BiomeTypeChanger fromVanilla(ResourceKey<Biome> target) {
        return new BiomeTypeChanger(
                b -> b.unwrapKey()
                        .map(k -> k.location().getNamespace().equals("minecraft"))
                        .orElse(false),
                target);
    }

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