package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Wire field order: {@code enabled} (the Spawn island's current BUILD_PROTECTION IslandSetting
 * value; {@code false} if the Spawn island doesn't exist yet — matches BUILD_PROTECTION's own
 * default), {@code authorizedPlayers} (list of {@link AuthorizedPlayerEntry}: the Spawn island's
 * MEMBER/CO_OWNER members — the same "real members" filter {@link AdminIslandBuilder}/
 * {@code IslandSnapshotBuilder} already use elsewhere — who can always build there regardless of
 * {@code enabled}, empty if the Spawn island doesn't exist).
 */
public record SpawnBuildProtectionStatusS2C(boolean enabled, List<AuthorizedPlayerEntry> authorizedPlayers) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnBuildProtectionStatusS2C> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_BUILD_PROTECTION_STATUS_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<AuthorizedPlayerEntry>> AUTHORIZED_PLAYERS_CODEC =
			ByteBufCodecs.collection(ArrayList::new, AuthorizedPlayerEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnBuildProtectionStatusS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SpawnBuildProtectionStatusS2C::enabled,
			AUTHORIZED_PLAYERS_CODEC, SpawnBuildProtectionStatusS2C::authorizedPlayers,
			SpawnBuildProtectionStatusS2C::new
	);

	public static SpawnBuildProtectionStatusS2C absent() {
		return new SpawnBuildProtectionStatusS2C(false, List.of());
	}

	@Override
	public CustomPacketPayload.Type<SpawnBuildProtectionStatusS2C> type() {
		return TYPE;
	}

	public record AuthorizedPlayerEntry(UUID uuid, String name, String role) {
		public static final StreamCodec<RegistryFriendlyByteBuf, AuthorizedPlayerEntry> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, AuthorizedPlayerEntry::uuid,
				ByteBufCodecs.STRING_UTF8, AuthorizedPlayerEntry::name,
				ByteBufCodecs.STRING_UTF8, AuthorizedPlayerEntry::role,
				AuthorizedPlayerEntry::new
		);
	}
}
