package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// targetUuid instead of a name: unlike invite (which may target an offline player the client
// only knows by name), trust always targets an existing member the client already has a UUID
// for from its own IslandSnapshotS2C member list.
public record MemberTrustC2S(UUID targetUuid) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MemberTrustC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.MEMBER_TRUST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, MemberTrustC2S> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, MemberTrustC2S::targetUuid,
			MemberTrustC2S::new
	);

	@Override
	public CustomPacketPayload.Type<MemberTrustC2S> type() {
		return TYPE;
	}
}
