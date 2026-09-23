package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of a normal island's own FlagSetPresetC2S, targeting the Spawn island instead
// of the sender's own (see IslandCoreMod.ISLAND_REGISTRY.applyFlagPreset in
// ServerPacketHandlers#registerSpawnAdminHandlers) — operator-gated instead of ownership-gated.
// Wire field order: flagId, preset ("nadie"/"miembros"/"aliados"/"todos").
public record SpawnFlagSetPresetC2S(String flagId, String preset) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpawnFlagSetPresetC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_FLAG_SET_PRESET_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnFlagSetPresetC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, SpawnFlagSetPresetC2S::flagId,
			ByteBufCodecs.STRING_UTF8, SpawnFlagSetPresetC2S::preset,
			SpawnFlagSetPresetC2S::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnFlagSetPresetC2S> type() {
		return TYPE;
	}
}
