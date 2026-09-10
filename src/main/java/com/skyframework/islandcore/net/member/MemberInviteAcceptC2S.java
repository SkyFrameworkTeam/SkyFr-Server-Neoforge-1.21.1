package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record MemberInviteAcceptC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MemberInviteAcceptC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.MEMBER_INVITE_ACCEPT_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, MemberInviteAcceptC2S> CODEC =
			StreamCodec.unit(new MemberInviteAcceptC2S());

	@Override
	public CustomPacketPayload.Type<MemberInviteAcceptC2S> type() {
		return TYPE;
	}
}
