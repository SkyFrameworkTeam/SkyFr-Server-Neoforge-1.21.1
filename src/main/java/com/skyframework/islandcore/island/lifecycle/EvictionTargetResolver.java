package com.skyframework.islandcore.island.lifecycle;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Optional;

// Shared by IslandDeletionServiceImpl and IslandCommand's kick: where to send a player evicted
// from an island (Sprint 12 pulled this out of IslandDeletionServiceImpl to avoid duplicating it).
public final class EvictionTargetResolver {

	private EvictionTargetResolver() {
	}

	public static Optional<EvictionTarget> resolve(MinecraftServer server) {
		Optional<Island> spawnIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (spawnIsland.isPresent()) {
			Island spawn = spawnIsland.get();
			ServerLevel world = server.getLevel(spawn.getDimension());
			if (world != null) {
				return Optional.of(new EvictionTarget(world, spawn.getHomeLocation()));
			}
		}

		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			return Optional.empty();
		}
		return Optional.of(new EvictionTarget(overworld, overworld.getSharedSpawnPos()));
	}

	public record EvictionTarget(ServerLevel world, BlockPos pos) {
	}
}
