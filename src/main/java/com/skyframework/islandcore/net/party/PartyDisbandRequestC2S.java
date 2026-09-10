package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose. Arms the same 15s confirmation window as "/party disband" (see
// PartyDisbandRequests) — an actual disband only happens once PartyDisbandConfirmC2S follows
// within that window.
public record PartyDisbandRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyDisbandRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.PARTY_DISBAND_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyDisbandRequestC2S> CODEC =
			StreamCodec.unit(new PartyDisbandRequestC2S());

	@Override
	public CustomPacketPayload.Type<PartyDisbandRequestC2S> type() {
		return TYPE;
	}
}
