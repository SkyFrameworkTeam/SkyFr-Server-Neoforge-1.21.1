package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record TeleportStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TeleportStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.TELEPORT_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleportStatusRequestC2S> CODEC =
			StreamCodec.unit(new TeleportStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<TeleportStatusRequestC2S> type() {
		return TYPE;
	}
}
