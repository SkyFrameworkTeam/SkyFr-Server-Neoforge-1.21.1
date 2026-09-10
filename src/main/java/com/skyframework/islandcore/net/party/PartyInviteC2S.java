package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// targetName instead of a UUID: same reasoning as member.MemberInviteC2S — the client has no
// reliable way to know an offline player's UUID up front. Resolved server-side by
// MembershipService#resolvePlayerUuid, the exact same name-resolution mechanism
// SpawnAuthorizedPlayerAddC2S already reuses.
public record PartyInviteC2S(String targetName) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyInviteC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.PARTY_INVITE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyInviteC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, PartyInviteC2S::targetName,
			PartyInviteC2S::new
	);

	@Override
	public CustomPacketPayload.Type<PartyInviteC2S> type() {
		return TYPE;
	}
}
