package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;
import com.skyframework.islandcore.net.flag.ExceptionGroupsStatusS2C;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

// Same shape and same ExceptionGroupsStatusS2C.GroupEntry as a normal island's own
// ExceptionGroupsStatusS2C (built by the exact same FlagsStatusBuilder#buildExceptionGroupsStatus,
// just given the Spawn island) — a SEPARATE payload id purely so the client routes it to its own
// cache instead of colliding with SettingsScreen's. Wire field order: groups (list of
// ExceptionGroupsStatusS2C.GroupEntry, same order/meaning as there).
public record SpawnExceptionGroupsStatusS2C(List<ExceptionGroupsStatusS2C.GroupEntry> groups) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnExceptionGroupsStatusS2C> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_EXCEPTION_GROUPS_STATUS_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<ExceptionGroupsStatusS2C.GroupEntry>> GROUP_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, ExceptionGroupsStatusS2C.GroupEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnExceptionGroupsStatusS2C> CODEC = StreamCodec.composite(
			GROUP_LIST_CODEC, SpawnExceptionGroupsStatusS2C::groups,
			SpawnExceptionGroupsStatusS2C::new
	);

	@Override
	public CustomPacketPayload.Type<SpawnExceptionGroupsStatusS2C> type() {
		return TYPE;
	}
}
