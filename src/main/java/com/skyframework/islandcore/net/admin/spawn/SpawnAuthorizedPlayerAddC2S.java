package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// targetName instead of a UUID: like MemberInviteC2S, the client has no reliable way to know an
// offline player's UUID up front. Resolved server-side by MembershipService#resolvePlayerUuid,
// which has no Brigadier GameProfileArgumentType to do this resolution for free like
// "/island admin spawn trust <player>" does.
public record SpawnAuthorizedPlayerAddC2S(String targetName) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnAuthorizedPlayerAddC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_AUTHORIZED_PLAYER_ADD_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnAuthorizedPlayerAddC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, SpawnAuthorizedPlayerAddC2S::targetName,
			SpawnAuthorizedPlayerAddC2S::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnAuthorizedPlayerAddC2S> type() {
		return TYPE;
	}
}
