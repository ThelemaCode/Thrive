package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forBS;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.forPos;
import static com.thelema.thrive.generic.blockentity.modifier.BiomeUtil.replaceStructure;

/**
 * 阶段式 BiomeModifier。
 * <p>
 * 子类构造里按执行顺序 {@link #addPhase}（或便捷方法 {@link #addBS}/{@link #addPos}/{@link #addFinish}），
 * 框架负责阶段索引推进、阶段切换时 tick 归 -1（由 {@link BiomeUtil#runTick} 的 +1 推回 0）、
 * 全部完成时返回 true。
 * <p>
 * <b>不继承它的 modifier 直接实现 {@link BiomeModifier}，可并行跑多个 finder。</b>
 * <p>
 * 进度存在 {@code be.data}：{@code "phase"} = 当前阶段索引，{@code "tick"} = 本阶段已跑 tick 数。
 */
public abstract class PhasedModifier implements BiomeModifier {

    @FunctionalInterface
    public interface Phase {
        /** 返回 true = 本阶段完成，索引前进。 */
        boolean run(BiomeCoreBlockEntity be);
    }

    private final List<Phase> phases = new ArrayList<>();

    /** 子类构造里按执行顺序调用。为空则第一 tick 即完成。 */
    protected void addPhase(Phase phase) {
        phases.add(phase);
    }

    /** forBS：遍历 + 改群系。 */
    protected void addBS(PosFinder finder, SectionApplier applier) {
        addPhase(b -> forBS(b, finder, applier));
    }

    /** forPos：遍历 + 改方块。 */
    protected void addPos(PosFinder finder, PosApplier applier) {
        addPhase(b -> forPos(b, finder, applier));
    }

    /** 收尾：替换结构，完成。 */
    protected void addFinish(ResourceLocation structure) {
        addPhase(b -> {
            replaceStructure((ServerLevel) b.getLevel(), b, structure);
            return true;
        });
    }

    @Override
    public boolean tick(BiomeCoreBlockEntity be) {
        int index = be.data.getInt("phase");
        if (index >= phases.size()) return true;

        boolean done = phases.get(index).run(be);
        if (!done) return false;

        int next = index + 1;
        be.data.putInt("phase", next);
        be.data.putLong("tick", -1);        // runTick 下一 tick +1 → 0
        return next >= phases.size();        // 无下一阶段 → true，BE 收尾
    }
}