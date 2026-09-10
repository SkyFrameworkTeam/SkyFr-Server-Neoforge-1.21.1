package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/dimension regenerate <id> confirm": calls the same
// DimensionRegistry#confirmRegeneration(id, senderUuid) the text command uses.
public record DimensionRegenerateConfirmC2S(String id) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DimensionRegenerateConfirmC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_REGENERATE_CONFIRM_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionRegenerateConfirmC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, DimensionRegenerateConfirmC2S::id,
			DimensionRegenerateConfirmC2S::new
	);

	@Override
	public CustomPacketPayload.Type<DimensionRegenerateConfirmC2S> type() {
		return TYPE;
	}
}
