package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// targetUuid instead of a name: unlike add (which may target an offline player the client only
// knows by name), remove always targets an existing entry from the authorizedPlayers list the
// client already fetched via SpawnBuildProtectionStatusS2C — same reasoning as MemberTrustC2S.
public record SpawnAuthorizedPlayerRemoveC2S(UUID targetUuid) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnAuthorizedPlayerRemoveC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_AUTHORIZED_PLAYER_REMOVE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnAuthorizedPlayerRemoveC2S> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, SpawnAuthorizedPlayerRemoveC2S::targetUuid,
			SpawnAuthorizedPlayerRemoveC2S::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnAuthorizedPlayerRemoveC2S> type() {
		return TYPE;
	}
}
