package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Network equivalent of "/dimension vanilla cancel <dimensionKey>": calls the same
// VanillaResetService#cancelPendingReset(dimensionKey) the text command uses. Cancels a QUEUED
// (already-confirmed) reset only — a still-counting-down PENDING_CONFIRMATION one is cancelled by
// simply letting the 30s window expire, same as every other confirmation flow in this codebase.
public record VanillaResetCancelC2S(String dimension) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<VanillaResetCancelC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.VANILLA_RESET_CANCEL_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, VanillaResetCancelC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, VanillaResetCancelC2S::dimension,
			VanillaResetCancelC2S::new
	);

	@Override
	public CustomPacketPayload.Type<VanillaResetCancelC2S> type() {
		return TYPE;
	}
}
