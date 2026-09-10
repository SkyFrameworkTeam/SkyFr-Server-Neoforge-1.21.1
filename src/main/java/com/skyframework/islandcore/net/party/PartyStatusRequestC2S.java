package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: the server identifies the requester from the connection itself
// (context.player()), same as IslandSnapshotRequestC2S.
public record PartyStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.PARTY_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyStatusRequestC2S> CODEC =
			StreamCodec.unit(new PartyStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<PartyStatusRequestC2S> type() {
		return TYPE;
	}
}
