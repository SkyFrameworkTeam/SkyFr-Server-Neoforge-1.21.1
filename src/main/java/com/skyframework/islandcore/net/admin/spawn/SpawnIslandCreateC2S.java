package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/island admin spawn create <size>": calls the same
// IslandRegistry#createSpawnIsland(ISLANDS_DIMENSION, size) the text command uses.
public record SpawnIslandCreateC2S(int size) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnIslandCreateC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_ISLAND_CREATE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnIslandCreateC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SpawnIslandCreateC2S::size,
			SpawnIslandCreateC2S::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnIslandCreateC2S> type() {
		return TYPE;
	}
}
