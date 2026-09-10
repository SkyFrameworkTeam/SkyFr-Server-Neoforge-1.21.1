package com.skyframework.islandcore.net.member;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// Covers both /island untrust and /island kick in one action: MembershipService#removeMember
// decides which behavior applies from the target's current role (see that method's own doc for
// the reasoning). The client only needs one "remove this member" button, not two.
public record MemberRemoveC2S(UUID targetUuid) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MemberRemoveC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.MEMBER_REMOVE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, MemberRemoveC2S> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, MemberRemoveC2S::targetUuid,
			MemberRemoveC2S::new
	);

	@Override
	public CustomPacketPayload.Type<MemberRemoveC2S> type() {
		return TYPE;
	}
}
