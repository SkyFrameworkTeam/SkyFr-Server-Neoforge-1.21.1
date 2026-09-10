package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, same as FlagsStatusRequestC2S.
public record ExceptionGroupsStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ExceptionGroupsStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.EXCEPTION_GROUPS_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, ExceptionGroupsStatusRequestC2S> CODEC =
			StreamCodec.unit(new ExceptionGroupsStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<ExceptionGroupsStatusRequestC2S> type() {
		return TYPE;
	}
}
