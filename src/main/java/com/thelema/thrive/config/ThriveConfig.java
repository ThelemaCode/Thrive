package com.thelema.thrive.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;


public class ThriveConfig {
    public static final ModConfigSpec SPEC;
    public static final ThriveConfig CONFIG;

    static {
        Pair<ThriveConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ThriveConfig::new);
        SPEC = pair.getRight();
        CONFIG = pair.getLeft();
    }

    ThriveConfig(ModConfigSpec.Builder builder) {


    }
}