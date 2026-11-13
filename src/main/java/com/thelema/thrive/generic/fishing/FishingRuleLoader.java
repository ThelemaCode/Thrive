package com.thelema.thrive.generic.fishing;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.thelema.thrive.ThriveMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ThriveMod.MOD_ID)
public class FishingRuleLoader {

    public static List<JsonObject> RULES = new ArrayList<>();

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(new Gson(), "fishing_loot") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
                List<JsonObject> newRules = new ArrayList<>();
                for (JsonElement element : object.values()) {
                    if (element.isJsonObject()) {
                        newRules.add(element.getAsJsonObject());
                    }
                }
                RULES = newRules;
                ThriveMod.LOGGER.info("Loaded {} fishing rules.", RULES.size());
            }
        });
    }
}