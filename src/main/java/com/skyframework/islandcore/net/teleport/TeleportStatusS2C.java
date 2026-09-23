package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Built by TeleportStatusBuilder from TeleportManager's new cooldown-peek getters and the
// existing Spawn/Rtp/FarmingConfig enabled flags — this record only carries data.
//
// Wire format changed (Sprint "teletransportes dinámicos"): the fixed 4th field `farming`
// (StatusEntry) was replaced by `dimensions` (List<DimensionTeleportEntry>) — one entry per
// DIMENSION_REGISTRY dimension instead of a single hardcoded farming slot. This is a client/server
// protocol break for IslandCoreClient's own copy of this record, which needs the matching update
// made separately in that project. Final field order: home, spawn, rtp, dimensions.
public record TeleportStatusS2C(StatusEntry home, StatusEntry spawn, StatusEntry rtp, List<DimensionTeleportEntry> dimensions) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<TeleportStatusS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.TELEPORT_STATUS_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<DimensionTeleportEntry>> DIMENSION_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, DimensionTeleportEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleportStatusS2C> CODEC = StreamCodec.composite(
			StatusEntry.CODEC, TeleportStatusS2C::home,
			StatusEntry.CODEC, TeleportStatusS2C::spawn,
			StatusEntry.CODEC, TeleportStatusS2C::rtp,
			DIMENSION_LIST_CODEC, TeleportStatusS2C::dimensions,
			TeleportStatusS2C::new
	);

	@Override
	public CustomPacketPayload.Type<TeleportStatusS2C> type() {
		return TYPE;
	}

	// reasonKey is only ever present when enabled == false: SPAWN_DISABLED/FARMING_DISABLED for
	// the two config-gated destinations, RTP_DISABLED/RTP_DIMENSION_NOT_ALLOWED for rtp. home
	// never sets it — there's no server-wide toggle for /island home.
	public record StatusEntry(boolean enabled, long cooldownRemainingSeconds, Optional<String> reasonKey) {

		private static final StreamCodec<ByteBuf, Optional<String>> REASON_CODEC = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8);

		public static final StreamCodec<RegistryFriendlyByteBuf, StatusEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, StatusEntry::enabled,
				ByteBufCodecs.VAR_LONG, StatusEntry::cooldownRemainingSeconds,
				REASON_CODEC, StatusEntry::reasonKey,
				StatusEntry::new
		);

		public static StatusEntry available(long cooldownRemainingSeconds) {
			return new StatusEntry(true, cooldownRemainingSeconds, Optional.empty());
		}

		public static StatusEntry unavailable(String reasonKey) {
			return new StatusEntry(false, 0, Optional.of(reasonKey));
		}
	}

	// id is the dimension's Identifier.toString() (e.g. "islandcore:farming") — the client sends it
	// straight back verbatim in TeleportRequestC2S's new dimensionId field, so there's no id<->name
	// resolution to keep in sync on either side. enabled/cooldownRemainingSeconds mirror the farming
	// dimension's own cooldown when isFarmingTarget (see TeleportStatusBuilder); every other
	// dimension has no cooldown of its own, so enabled is always true and cooldownRemainingSeconds 0.
	public record DimensionTeleportEntry(String id, String displayName, boolean enabled, long cooldownRemainingSeconds) {

		public static final StreamCodec<RegistryFriendlyByteBuf, DimensionTeleportEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, DimensionTeleportEntry::id,
				ByteBufCodecs.STRING_UTF8, DimensionTeleportEntry::displayName,
				ByteBufCodecs.BOOL, DimensionTeleportEntry::enabled,
				ByteBufCodecs.VAR_LONG, DimensionTeleportEntry::cooldownRemainingSeconds,
				DimensionTeleportEntry::new
		);
	}
}
