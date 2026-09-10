package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// id is the dimension's path only (e.g. "foo" for "islandcore:foo"), matching the "id" argument
// DimensionCommand's Brigadier tree already takes — the handler builds the full ResourceLocation the
// same way DimensionCommand#executeInfo does (ResourceLocation.of("islandcore", id)).
public record DimensionDetailRequestC2S(String id) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DimensionDetailRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_DETAIL_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionDetailRequestC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, DimensionDetailRequestC2S::id,
			DimensionDetailRequestC2S::new
	);

	@Override
	public CustomPacketPayload.Type<DimensionDetailRequestC2S> type() {
		return TYPE;
	}
}
