package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/island admin spawn settings buildprotection <value>": delegates to
// IslandRegistry#updateIslandSetting(spawnIslandId, IslandSetting.BUILD_PROTECTION, enabled), the
// same call the text command makes.
public record SpawnBuildProtectionSetC2S(boolean enabled) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnBuildProtectionSetC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_BUILD_PROTECTION_SET_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnBuildProtectionSetC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SpawnBuildProtectionSetC2S::enabled,
			SpawnBuildProtectionSetC2S::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnBuildProtectionSetC2S> type() {
		return TYPE;
	}
}
