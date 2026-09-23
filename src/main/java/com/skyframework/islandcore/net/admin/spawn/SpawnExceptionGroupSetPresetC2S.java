package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/island admin spawn exceptions preset <group> <preset>"
// (executeAdminSpawnExceptionsPreset) — same IslandCoreMod.ISLAND_REGISTRY.applyExceptionGroupPreset
// call, reachable from SpawnManagerScreen instead of only the command. Wire field order: groupId,
// preset ("nadie"/"miembros"/"aliados"/"todos").
public record SpawnExceptionGroupSetPresetC2S(String groupId, String preset) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpawnExceptionGroupSetPresetC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_EXCEPTION_GROUP_SET_PRESET_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnExceptionGroupSetPresetC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, SpawnExceptionGroupSetPresetC2S::groupId,
			ByteBufCodecs.STRING_UTF8, SpawnExceptionGroupSetPresetC2S::preset,
			SpawnExceptionGroupSetPresetC2S::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnExceptionGroupSetPresetC2S> type() {
		return TYPE;
	}
}
