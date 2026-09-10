package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, same shape as FlagsStatusRequestC2S — requests the current server-wide
// defaults (not any specific island's), see AdminDefaultsStatusS2C. Operator-only.
public record AdminDefaultsStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AdminDefaultsStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_DEFAULTS_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminDefaultsStatusRequestC2S> CODEC =
			StreamCodec.unit(new AdminDefaultsStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<AdminDefaultsStatusRequestC2S> type() {
		return TYPE;
	}
}
