package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// targetUuid instead of a name: unlike MemberAllyAddC2S, removing an ally always targets an
// existing ALLY entry the client already has a UUID for from its own IslandSnapshotS2C member
// list — same reasoning as MemberTrustC2S/MemberRemoveC2S.
public record MemberAllyRemoveC2S(UUID targetUuid) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MemberAllyRemoveC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.MEMBER_ALLY_REMOVE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, MemberAllyRemoveC2S> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, MemberAllyRemoveC2S::targetUuid,
			MemberAllyRemoveC2S::new
	);

	@Override
	public CustomPacketPayload.Type<MemberAllyRemoveC2S> type() {
		return TYPE;
	}
}
