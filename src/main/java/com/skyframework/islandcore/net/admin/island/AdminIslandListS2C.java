package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One page of the admin island list, built by {@link AdminIslandBuilder}. Wire field order:
 * {@code islands} (list of {@link IslandEntry}), {@code totalPages} (int, 0 if the filtered result
 * is empty), {@code currentPage} (int, the same 0-indexed page the request asked for, clamped into
 * {@code [0, totalPages - 1]} if it was out of range).
 */
public record AdminIslandListS2C(List<IslandEntry> islands, int totalPages, int currentPage) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<AdminIslandListS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_ISLAND_LIST_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<IslandEntry>> ISLAND_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, IslandEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminIslandListS2C> CODEC = StreamCodec.composite(
			ISLAND_LIST_CODEC, AdminIslandListS2C::islands,
			ByteBufCodecs.VAR_INT, AdminIslandListS2C::totalPages,
			ByteBufCodecs.VAR_INT, AdminIslandListS2C::currentPage,
			AdminIslandListS2C::new
	);

	@Override
	public CustomPacketPayload.Type<AdminIslandListS2C> type() {
		return TYPE;
	}

	/**
	 * One row of the list. Wire field order: {@code ownerUuid}, {@code ownerName} (resolved via
	 * the server's user cache, falls back to the raw UUID string if unresolved), {@code size},
	 * {@code maxSize} (from {@code PermissionProvider#getHighestSizeAllowed}), {@code type} (island
	 * type id), {@code currentBiomeId} (a LIVE lookup of the biome at the island's center block,
	 * not the persisted {@code IslandData#currentBiomeId} tracking field — see
	 * {@link AdminIslandBuilder}), {@code state}, {@code memberCount} (MEMBER/CO_OWNER roles only,
	 * owner not counted — same filter {@code IslandMessages}/{@code IslandSnapshotBuilder} already
	 * apply to member lists), {@code isSpawnIsland} (NEW — true when {@code ownerUuid} is
	 * {@code Island.SERVER_OWNER_UUID}; see {@link AdminIslandBuilder#buildList} for how this row
	 * gets prepended ahead of pagination on page 0 with no active search).
	 */
	public record IslandEntry(
			UUID ownerUuid,
			String ownerName,
			int size,
			int maxSize,
			String type,
			String currentBiomeId,
			String state,
			int memberCount,
			boolean isSpawnIsland
	) {
		public static final StreamCodec<RegistryFriendlyByteBuf, IslandEntry> CODEC = StreamCodec.of(
				(buf, value) -> {
					UUIDUtil.STREAM_CODEC.encode(buf, value.ownerUuid());
					ByteBufCodecs.STRING_UTF8.encode(buf, value.ownerName());
					ByteBufCodecs.VAR_INT.encode(buf, value.size());
					ByteBufCodecs.VAR_INT.encode(buf, value.maxSize());
					ByteBufCodecs.STRING_UTF8.encode(buf, value.type());
					ByteBufCodecs.STRING_UTF8.encode(buf, value.currentBiomeId());
					ByteBufCodecs.STRING_UTF8.encode(buf, value.state());
					ByteBufCodecs.VAR_INT.encode(buf, value.memberCount());
					ByteBufCodecs.BOOL.encode(buf, value.isSpawnIsland());
				},
				buf -> new IslandEntry(
						UUIDUtil.STREAM_CODEC.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf),
						ByteBufCodecs.VAR_INT.decode(buf),
						ByteBufCodecs.BOOL.decode(buf)
				)
		);
	}
}
