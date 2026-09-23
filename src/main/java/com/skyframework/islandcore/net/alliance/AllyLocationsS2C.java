package com.skyframework.islandcore.net.alliance;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Pushed periodically by AllyLocationBroadcaster (not requested by the client) to every player with
// receivePositions=true, one entry per eligible allied-island player currently online, in the same
// dimension as the receiver, with sendPosition=true — see AllyLocationBroadcaster for the full
// eligibility rule. entries is empty (never omitted entirely) when the receiver has no eligible
// allies right now, so the client can clear any stale HUD indicator instead of leaving it frozen on
// a last-known position. No dimension field: every entry is already guaranteed to share the
// receiver's own current dimension.
public record AllyLocationsS2C(List<Entry> entries) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AllyLocationsS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.ALLY_LOCATIONS_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<Entry>> ENTRY_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, Entry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, AllyLocationsS2C> CODEC = StreamCodec.composite(
			ENTRY_LIST_CODEC, AllyLocationsS2C::entries,
			AllyLocationsS2C::new
	);

	@Override
	public CustomPacketPayload.Type<AllyLocationsS2C> type() {
		return TYPE;
	}

	public record Entry(UUID uuid, String name, double x, double y, double z) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Entry::uuid,
				ByteBufCodecs.STRING_UTF8, Entry::name,
				ByteBufCodecs.DOUBLE, Entry::x,
				ByteBufCodecs.DOUBLE, Entry::y,
				ByteBufCodecs.DOUBLE, Entry::z,
				Entry::new
		);
	}
}
