package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// targetName instead of a UUID: the client has no reliable way to know an offline player's UUID
// up front. Resolved server-side by MembershipService#inviteByName, which has no Brigadier
// GameProfileArgumentType to do this resolution for free like the text command does.
public record MemberInviteC2S(String targetName) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MemberInviteC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.MEMBER_INVITE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, MemberInviteC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, MemberInviteC2S::targetName,
			MemberInviteC2S::new
	);

	@Override
	public CustomPacketPayload.Type<MemberInviteC2S> type() {
		return TYPE;
	}
}
