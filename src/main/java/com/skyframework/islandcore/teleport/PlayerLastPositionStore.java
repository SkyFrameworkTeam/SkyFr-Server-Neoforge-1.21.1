package com.skyframework.islandcore.teleport;

import com.skyframework.islandcore.IslandCoreMod;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Per-player, per-dimension last position (Sprint "teletransportes dinámicos"): recorded by
// PlayerLastPositionMixin right before a player leaves a dimension, and consulted by
// TeleportManagerImpl#requestDimensionTeleport so a player's second-or-later visit to a dynamic
// dimension resumes where they left off instead of always landing back at the world's spawn point.
// Same single-shared-file, load-on-SERVER_STARTED, save-on-every-write pattern as
// PlayerLocationSharingConfig (<world>/islandcore/last_positions.dat).
public class PlayerLastPositionStore {

	private static final String FILE_NAME = "last_positions.dat";

	private final Map<UUID, Map<ResourceLocation, BlockPos>> byPlayer = new HashMap<>();

	private Path file;

	public PlayerLastPositionStore() {
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) ->
				load(event.getServer().getWorldPath(LevelResource.ROOT).resolve("islandcore").resolve(FILE_NAME)));
	}

	private void load(Path file) {
		this.file = file;

		if (!Files.exists(file)) {
			return;
		}

		try {
			CompoundTag nbt = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			for (String playerKey : nbt.getAllKeys()) {
				try {
					UUID playerUuid = UUID.fromString(playerKey);
					CompoundTag perDimension = nbt.getCompound(playerKey);
					Map<ResourceLocation, BlockPos> positions = new HashMap<>();
					for (String dimensionKey : perDimension.getAllKeys()) {
						try {
							ResourceLocation dimensionId = ResourceLocation.parse(dimensionKey);
							CompoundTag posNbt = perDimension.getCompound(dimensionKey);
							positions.put(dimensionId, new BlockPos(posNbt.getInt("x"), posNbt.getInt("y"), posNbt.getInt("z")));
						} catch (RuntimeException e) {
							IslandCoreMod.LOGGER.error("Skipping invalid dimension key in {}: {}", FILE_NAME, dimensionKey, e);
						}
					}
					byPlayer.put(playerUuid, positions);
				} catch (IllegalArgumentException e) {
					IslandCoreMod.LOGGER.error("Skipping invalid UUID key in {}: {}", FILE_NAME, playerKey, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			IslandCoreMod.LOGGER.error("Failed to load {}, every player starts with no remembered position", FILE_NAME, e);
		}
	}

	public Optional<BlockPos> getLastPosition(UUID playerUuid, ResourceLocation dimensionId) {
		Map<ResourceLocation, BlockPos> positions = byPlayer.get(playerUuid);
		if (positions == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(positions.get(dimensionId));
	}

	public void recordLastPosition(UUID playerUuid, ResourceLocation dimensionId, BlockPos pos) {
		byPlayer.computeIfAbsent(playerUuid, uuid -> new HashMap<>()).put(dimensionId, pos);
		save();
	}

	private void save() {
		if (file == null) {
			return;
		}

		CompoundTag nbt = new CompoundTag();
		for (Map.Entry<UUID, Map<ResourceLocation, BlockPos>> playerEntry : byPlayer.entrySet()) {
			CompoundTag perDimension = new CompoundTag();
			for (Map.Entry<ResourceLocation, BlockPos> dimensionEntry : playerEntry.getValue().entrySet()) {
				CompoundTag posNbt = new CompoundTag();
				BlockPos pos = dimensionEntry.getValue();
				posNbt.putInt("x", pos.getX());
				posNbt.putInt("y", pos.getY());
				posNbt.putInt("z", pos.getZ());
				perDimension.put(dimensionEntry.getKey().toString(), posNbt);
			}
			nbt.put(playerEntry.getKey().toString(), perDimension);
		}

		try {
			Files.createDirectories(file.getParent());
			NbtIo.writeCompressed(nbt, file);
		} catch (IOException e) {
			IslandCoreMod.LOGGER.error("Failed to save {}", FILE_NAME, e);
		}
	}
}
