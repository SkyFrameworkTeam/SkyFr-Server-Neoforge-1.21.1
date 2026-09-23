package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, like SpawnStatusRequestC2S: the server acts on
// IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID), never on
// client-supplied data. Replies with the SAME FlagsStatusS2C a normal island's own
// FlagsStatusRequestC2S replies with (FlagsStatusBuilder#buildFlagsStatus already takes an Island
// directly, so it's fully reusable here) — see ServerPacketHandlers#registerSpawnAdminHandlers.
public record SpawnFlagsStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpawnFlagsStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_FLAGS_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnFlagsStatusRequestC2S> CODEC =
			StreamCodec.unit(new SpawnFlagsStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<SpawnFlagsStatusRequestC2S> type() {
		return TYPE;
	}
}
