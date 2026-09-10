package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// targetUuid instead of a name: the target is always an existing party member the client already
// has a UUID for from its own PartyStatusS2C member list — same reasoning as member.MemberTrustC2S.
public record PartyKickC2S(UUID targetUuid) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyKickC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.PARTY_KICK_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyKickC2S> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, PartyKickC2S::targetUuid,
			PartyKickC2S::new
	);

	@Override
	public CustomPacketPayload.Type<PartyKickC2S> type() {
		return TYPE;
	}
}
