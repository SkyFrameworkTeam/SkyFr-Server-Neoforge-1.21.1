package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Empty on purpose, like SpawnFlagsStatusRequestC2S. Replies with the SAME ExceptionGroupsStatusS2C
// a normal island's own ExceptionGroupsStatusRequestC2S replies with
// (FlagsStatusBuilder#buildExceptionGroupsStatus already takes an Island directly, no playerUuid at
// all, so it's fully reusable here).
public record SpawnExceptionGroupsStatusRequestC2S() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<SpawnExceptionGroupsStatusRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_EXCEPTION_GROUPS_STATUS_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnExceptionGroupsStatusRequestC2S> CODEC =
			StreamCodec.unit(new SpawnExceptionGroupsStatusRequestC2S());

	@Override
	public CustomPacketPayload.Type<SpawnExceptionGroupsStatusRequestC2S> type() {
		return TYPE;
	}
}
