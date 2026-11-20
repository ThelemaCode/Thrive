package com.thelema.thrive.generic.blockentity.modifier;

import com.thelema.thrive.generic.blockentity.BiomeCoreBlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 阶段式 BiomeModifier。
 */
public abstract class PhasedModifier implements BiomeModifier {

    @FunctionalInterface
    public interface Phase {
        /** 返回 true = 本阶段完成，索引前进。 */
        boolean run(BiomeCoreBlockEntity be);
    }

    private final List<Phase> phases = new ArrayList<>();

    /** 子类构造里按执行顺序调用。为空则第一 tick 即完成。 */
    public void addPhase(Phase phase) {
        phases.add(phase);
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