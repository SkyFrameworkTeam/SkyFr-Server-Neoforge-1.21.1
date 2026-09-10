package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, like SpawnStatusRequestC2S: the server acts on
// IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID), never on
// client-supplied data.
public record SpawnBuildProtectionStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpawnBuildProtectionStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_BUILD_PROTECTION_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnBuildProtectionStatusRequestC2S> CODEC =
			StreamCodec.unit(new SpawnBuildProtectionStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<SpawnBuildProtectionStatusRequestC2S> type() {
		return TYPE;
	}
}
