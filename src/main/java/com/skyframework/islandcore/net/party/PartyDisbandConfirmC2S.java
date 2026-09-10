package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose. Only succeeds if PartyDisbandRequestC2S (or "/party disband") armed the
// confirmation window within the last 15s — see PartyDisbandRequests.
public record PartyDisbandConfirmC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyDisbandConfirmC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.PARTY_DISBAND_CONFIRM_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyDisbandConfirmC2S> CODEC =
			StreamCodec.unit(new PartyDisbandConfirmC2S());

	@Override
	public CustomPacketPayload.Type<PartyDisbandConfirmC2S> type() {
		return TYPE;
	}
}
