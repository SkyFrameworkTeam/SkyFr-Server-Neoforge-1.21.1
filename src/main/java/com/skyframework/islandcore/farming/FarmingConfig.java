package com.skyframework.islandcore.farming;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.skyframework.islandcore.IslandCoreMod;

import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// Loaded from config/islandcore/farming.json on SERVER_STARTED, same pattern as SpawnConfig.
public class FarmingConfig {

	private static final String CONFIG_FILE_NAME = "farming.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final ResourceLocation DEFAULT_TARGET_DIMENSION = ResourceLocation.fromNamespaceAndPath("islandcore", "farming");

	private boolean enabled = true;
	private ResourceLocation targetDimension = DEFAULT_TARGET_DIMENSION;

	public FarmingConfig() {
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> load());
	}

	private void load() {
		Path configFile = FMLPaths.CONFIGDIR.get().resolve("islandcore").resolve(CONFIG_FILE_NAME);

		if (!Files.exists(configFile)) {
			writeDefault(configFile);
		}

		try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			if (root.has("enabled")) {
				enabled = root.get("enabled").getAsBoolean();
			}
			if (root.has("targetDimension")) {
				try {
					targetDimension = ResourceLocation.parse(root.get("targetDimension").getAsString());
				} catch (RuntimeException e) {
					IslandCoreMod.LOGGER.error("Invalid targetDimension in {}, keeping default", CONFIG_FILE_NAME, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, using defaults", CONFIG_FILE_NAME, e);
		}
	}

	private void writeDefault(Path configFile) {
		JsonObject root = new JsonObject();
		root.addProperty("enabled", true);
		root.addProperty("targetDimension", DEFAULT_TARGET_DIMENSION.toString());

		try {
			Files.createDirectories(configFile.getParent());
			Files.writeString(configFile, GSON.toJson(root));
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to create default {}", CONFIG_FILE_NAME, e);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public ResourceLocation getTargetDimension() {
		return targetDimension;
	}
}
