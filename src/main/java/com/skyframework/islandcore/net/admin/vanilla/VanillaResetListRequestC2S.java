package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose: the server acts on VanillaResetService#listPendingResets(), never on
// client-supplied data.
public record VanillaResetListRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<VanillaResetListRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.VANILLA_RESET_LIST_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, VanillaResetListRequestC2S> CODEC =
			StreamCodec.unit(new VanillaResetListRequestC2S());

	@Override
	public CustomPacketPayload.Type<VanillaResetListRequestC2S> type() {
		return TYPE;
	}
}
