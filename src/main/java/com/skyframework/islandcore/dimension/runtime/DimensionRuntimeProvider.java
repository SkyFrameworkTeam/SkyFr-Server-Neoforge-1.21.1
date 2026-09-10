package com.skyframework.islandcore.dimension.runtime;

import com.skyframework.islandcore.dimension.model.DimensionDefinition;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;

public interface DimensionRuntimeProvider {

	ServerLevel createOrLoadWorld(DimensionDefinition definition, MinecraftServer server);

	// Queues the deletion; does not block. The implementation is expected to wait until the
	// dimension has no players/loaded chunks before actually removing it.
	void unloadAndDeleteWorld(ResourceLocation id, MinecraftServer server);
}
