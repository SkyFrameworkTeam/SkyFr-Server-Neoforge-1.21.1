package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: the server acts on context.player(), same as MemberInviteAcceptC2S.
public record PartyAcceptC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyAcceptC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.PARTY_ACCEPT_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyAcceptC2S> CODEC =
			StreamCodec.unit(new PartyAcceptC2S());

	@Override
	public CustomPacketPayload.Type<PartyAcceptC2S> type() {
		return TYPE;
	}
}
