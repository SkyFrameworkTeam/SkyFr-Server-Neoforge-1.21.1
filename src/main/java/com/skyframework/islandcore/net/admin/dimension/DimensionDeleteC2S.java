package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/dimension delete <id>": calls the same
// DimensionRegistry#requestDeletion(id, senderUuid) the text command uses.
public record DimensionDeleteC2S(String id) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DimensionDeleteC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_DELETE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionDeleteC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, DimensionDeleteC2S::id,
			DimensionDeleteC2S::new
	);

	@Override
	public CustomPacketPayload.Type<DimensionDeleteC2S> type() {
		return TYPE;
	}
}
