package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, like MemberInviteAcceptC2S: the server acts on context.player(), never on
// client-supplied data.
public record MemberInviteDeclineC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MemberInviteDeclineC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.MEMBER_INVITE_DECLINE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, MemberInviteDeclineC2S> CODEC =
			StreamCodec.unit(new MemberInviteDeclineC2S());

	@Override
	public CustomPacketPayload.Type<MemberInviteDeclineC2S> type() {
		return TYPE;
	}
}
