package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record IslandUpgradeC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<IslandUpgradeC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.ISLAND_UPGRADE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, IslandUpgradeC2S> CODEC = StreamCodec.unit(new IslandUpgradeC2S());

	@Override
	public CustomPacketPayload.Type<IslandUpgradeC2S> type() {
		return TYPE;
	}
}
