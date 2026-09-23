package com.skyframework.islandcore.net.alliance;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: the server identifies the requester from the connection itself
// (context.player()), same as AllianceStatusRequestC2S.
public record LocationSharingStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LocationSharingStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.LOCATION_SHARING_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, LocationSharingStatusRequestC2S> CODEC =
			StreamCodec.unit(new LocationSharingStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<LocationSharingStatusRequestC2S> type() {
		return TYPE;
	}
}
