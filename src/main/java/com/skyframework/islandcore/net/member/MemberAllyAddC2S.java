package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// targetName instead of a UUID: an ally, unlike trust/remove's targets, doesn't have to already be
// a member the client has a UUID for from its own IslandSnapshotS2C member list — same reasoning
// as MemberInviteC2S. Resolved server-side by MembershipService#resolvePlayerUuid, the exact same
// name-resolution mechanism MembershipService#inviteByName already reuses for MemberInviteC2S.
public record MemberAllyAddC2S(String targetName) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MemberAllyAddC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.MEMBER_ALLY_ADD_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, MemberAllyAddC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, MemberAllyAddC2S::targetName,
			MemberAllyAddC2S::new
	);

	@Override
	public CustomPacketPayload.Type<MemberAllyAddC2S> type() {
		return TYPE;
	}
}
