package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/island admin spawn resize <size>": calls the same
// IslandRegistry#resizeIsland(spawnIslandId, newSize) the text command uses.
public record SpawnIslandResizeC2S(int newSize) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnIslandResizeC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_ISLAND_RESIZE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnIslandResizeC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SpawnIslandResizeC2S::newSize,
			SpawnIslandResizeC2S::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnIslandResizeC2S> type() {
		return TYPE;
	}
}
