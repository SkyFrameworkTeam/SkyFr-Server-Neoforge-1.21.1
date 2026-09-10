package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: the server identifies the requester from the connection itself
// (context.player()) and looks up their own island, same as IslandSnapshotRequestC2S.
public record FlagsStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FlagsStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.FLAGS_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, FlagsStatusRequestC2S> CODEC =
			StreamCodec.unit(new FlagsStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<FlagsStatusRequestC2S> type() {
		return TYPE;
	}
}
