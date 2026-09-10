package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: the server acts on DimensionRegistry#getAllDimensions(), never on
// client-supplied data. No pagination here (unlike the island list) — matches
// "/dimension list" itself, which doesn't paginate either.
public record DimensionListRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DimensionListRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_LIST_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionListRequestC2S> CODEC =
			StreamCodec.unit(new DimensionListRequestC2S());

	@Override
	public CustomPacketPayload.Type<DimensionListRequestC2S> type() {
		return TYPE;
	}
}
