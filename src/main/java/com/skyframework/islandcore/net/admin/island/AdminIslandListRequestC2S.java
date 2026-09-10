package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Requests one page of the admin island list. Wire field order: {@code page} (int, 0-indexed —
 * page 0 is the first page), {@code pageSize} (int, clamped to at least 1 server-side),
 * {@code searchQuery} (String, empty meaning "no filter" — filtered against the resolved owner
 * name, not the raw UUID). See {@link AdminIslandListS2C} for the reply.
 */
public record AdminIslandListRequestC2S(int page, int pageSize, String searchQuery) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<AdminIslandListRequestC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_ISLAND_LIST_REQUEST_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminIslandListRequestC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, AdminIslandListRequestC2S::page,
			ByteBufCodecs.VAR_INT, AdminIslandListRequestC2S::pageSize,
			ByteBufCodecs.STRING_UTF8, AdminIslandListRequestC2S::searchQuery,
			AdminIslandListRequestC2S::new
	);

	@Override
	public CustomPacketPayload.Type<AdminIslandListRequestC2S> type() {
		return TYPE;
	}
}
