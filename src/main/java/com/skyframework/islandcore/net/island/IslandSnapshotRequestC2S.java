package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: the server identifies the requester from the connection itself
// (context.player()), it never needs the client to tell it who's asking.
public record IslandSnapshotRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<IslandSnapshotRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ISLAND_SNAPSHOT_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, IslandSnapshotRequestC2S> CODEC =
			StreamCodec.unit(new IslandSnapshotRequestC2S());

	@Override
	public CustomPacketPayload.Type<IslandSnapshotRequestC2S> type() {
		return TYPE;
	}
}
