package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of a normal island's own FlagSetC2S, targeting the Spawn island instead of the
// sender's own (see IslandCoreMod.ISLAND_REGISTRY.updateRoleFlagOverride/updateGlobalFlagOverride
// in ServerPacketHandlers#registerSpawnAdminHandlers) — operator-gated instead of
// ownership/FLAG_PERMISSION_REQUIREMENTS-gated, since IslandActionService#updateFlag resolves the
// island from the ACTING player's own UUID and can't be reused as-is for an admin acting on Spawn.
// Wire field order: flagId, value ("allow"/"deny"/"default", same TriState vocabulary FlagSetC2S
// already uses).
public record SpawnFlagSetC2S(String flagId, String value) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpawnFlagSetC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_FLAG_SET_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnFlagSetC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, SpawnFlagSetC2S::flagId,
			ByteBufCodecs.STRING_UTF8, SpawnFlagSetC2S::value,
			SpawnFlagSetC2S::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnFlagSetC2S> type() {
		return TYPE;
	}
}
