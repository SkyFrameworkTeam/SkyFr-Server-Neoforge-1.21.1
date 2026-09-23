package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;
import com.skyframework.islandcore.net.flag.FlagsStatusS2C;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

// Same shape and same FlagsStatusS2C.FlagEntry as a normal island's own FlagsStatusS2C (built by
// the exact same FlagsStatusBuilder#buildFlagsStatus, just given the Spawn island) — a SEPARATE
// payload id purely so the client routes it to its own cache instead of colliding with
// SettingsScreen's (which reads FlagsStatusS2C for the ACTING player's own island). Wire field
// order: flags (list of FlagsStatusS2C.FlagEntry, same order/meaning as there).
public record SpawnFlagsStatusS2C(List<FlagsStatusS2C.FlagEntry> flags) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnFlagsStatusS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_FLAGS_STATUS_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<FlagsStatusS2C.FlagEntry>> FLAG_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, FlagsStatusS2C.FlagEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnFlagsStatusS2C> CODEC = StreamCodec.composite(
			FLAG_LIST_CODEC, SpawnFlagsStatusS2C::flags,
			SpawnFlagsStatusS2C::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnFlagsStatusS2C> type() {
		return TYPE;
	}
}
