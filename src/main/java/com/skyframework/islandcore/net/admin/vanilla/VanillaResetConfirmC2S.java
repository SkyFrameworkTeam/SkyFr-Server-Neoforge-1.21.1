package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/dimension vanilla regenerate <dimensionKey> confirm": calls the same
// VanillaResetService#confirmReset(dimensionKey, senderUuid) the text command uses.
public record VanillaResetConfirmC2S(String dimension) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<VanillaResetConfirmC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.VANILLA_RESET_CONFIRM_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, VanillaResetConfirmC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, VanillaResetConfirmC2S::dimension,
			VanillaResetConfirmC2S::new
	);

	@Override
	public CustomPacketPayload.Type<VanillaResetConfirmC2S> type() {
		return TYPE;
	}
}
