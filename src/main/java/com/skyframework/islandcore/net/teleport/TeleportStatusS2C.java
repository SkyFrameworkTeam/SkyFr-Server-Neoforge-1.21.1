package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

// Built by TeleportStatusBuilder from TeleportManager's new cooldown-peek getters and the
// existing Spawn/Rtp/FarmingConfig enabled flags — this record only carries data.
public record TeleportStatusS2C(StatusEntry home, StatusEntry spawn, StatusEntry rtp, StatusEntry farming) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<TeleportStatusS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.TELEPORT_STATUS_S2C);

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleportStatusS2C> CODEC = StreamCodec.composite(
			StatusEntry.CODEC, TeleportStatusS2C::home,
			StatusEntry.CODEC, TeleportStatusS2C::spawn,
			StatusEntry.CODEC, TeleportStatusS2C::rtp,
			StatusEntry.CODEC, TeleportStatusS2C::farming,
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
}
