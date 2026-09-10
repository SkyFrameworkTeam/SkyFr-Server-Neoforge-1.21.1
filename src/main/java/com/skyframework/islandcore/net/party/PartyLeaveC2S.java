package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: the server acts on context.player().
public record PartyLeaveC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyLeaveC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.PARTY_LEAVE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyLeaveC2S> CODEC =
			StreamCodec.unit(new PartyLeaveC2S());

	@Override
	public CustomPacketPayload.Type<PartyLeaveC2S> type() {
		return TYPE;
	}
}
