package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

// targetUuid is the island OWNER's uuid (same lookup key as /island admin list <player>), not the
// islandId — the admin-facing list/detail flow only ever knows players, matching how
// IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner is already used by the equivalent text commands.
public record AdminIslandDetailRequestC2S(UUID targetUuid) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<AdminIslandDetailRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_ISLAND_DETAIL_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminIslandDetailRequestC2S> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, AdminIslandDetailRequestC2S::targetUuid,
			AdminIslandDetailRequestC2S::new
	);

	@Override
	public CustomPacketPayload.Type<AdminIslandDetailRequestC2S> type() {
		return TYPE;
	}
}
