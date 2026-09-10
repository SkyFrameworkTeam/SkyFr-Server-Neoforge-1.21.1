package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/dimension delete <id> confirm": calls the same
// DimensionRegistry#confirmDeletion(id, senderUuid) the text command uses.
public record DimensionDeleteConfirmC2S(String id) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DimensionDeleteConfirmC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_DELETE_CONFIRM_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionDeleteConfirmC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, DimensionDeleteConfirmC2S::id,
			DimensionDeleteConfirmC2S::new
	);

	@Override
	public CustomPacketPayload.Type<DimensionDeleteConfirmC2S> type() {
		return TYPE;
	}
}
