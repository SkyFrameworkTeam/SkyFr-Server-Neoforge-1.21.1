package com.skyframework.islandcore.net.biome;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, like IslandSnapshotRequestC2S: the server acts on context.player(), never on
// client-supplied data.
public record BiomeTiersRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BiomeTiersRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.BIOME_TIERS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, BiomeTiersRequestC2S> CODEC =
			StreamCodec.unit(new BiomeTiersRequestC2S());

	@Override
	public CustomPacketPayload.Type<BiomeTiersRequestC2S> type() {
		return TYPE;
	}
}
