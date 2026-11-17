package com.thelema.thrive.register;

import com.thelema.thrive.ThriveMod;
import com.thelema.thrive.generic.structure.OnlyReplaceableProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 结构处理器（{@code StructureProcessor}）注册。
 *
 * <p>注册不只是为了好看：{@code StructureProcessor.getType()} 是抽象方法，
 * 不注册就没有可以返回的类型。注册之后同时也能在 JSON 的 {@code processor_list} 里用：
 * <pre>
 * { "processors": [ { "processor_type": "thrive:only_replaceable" } ] }
 * </pre>
 */
public class ProcessorRegister {

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSOR =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, ThriveMod.MOD_ID);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<OnlyReplaceableProcessor>>
            ONLY_REPLACEABLE = PROCESSOR.register("only_replaceable", () -> () -> OnlyReplaceableProcessor.CODEC);

    public static void register(IEventBus bus) {
        PROCESSOR.register(bus);
    }
}
