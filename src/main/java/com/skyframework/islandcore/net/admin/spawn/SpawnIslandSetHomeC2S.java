package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: no coordinates travel over the wire. The handler reads the ACTUAL sender's
// player.blockPosition() at the moment the packet is received, applying the exact same
// dimension+bounds validation "/island admin spawn sethome" already does inline before calling
// IslandRegistry#updateHomeLocation — see ServerPacketHandlers for that check. Failure (not inside
// the Spawn island's built bounds) replies with ActionResultS2C.fail(ActionReason.UNSAFE_LOCATION).
public record SpawnIslandSetHomeC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpawnIslandSetHomeC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_ISLAND_SET_HOME_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnIslandSetHomeC2S> CODEC =
			StreamCodec.unit(new SpawnIslandSetHomeC2S());

	@Override
	public CustomPacketPayload.Type<SpawnIslandSetHomeC2S> type() {
		return TYPE;
	}
}
