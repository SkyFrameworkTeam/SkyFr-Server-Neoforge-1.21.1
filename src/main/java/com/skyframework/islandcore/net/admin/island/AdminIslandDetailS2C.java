package com.skyframework.islandcore.net.admin.island;

import com.skyframework.islandcore.net.NetworkChannels;
import com.skyframework.islandcore.net.island.IslandSnapshotS2C;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Same level of detail as {@code /island admin list <player>} ({@code IslandMessages
 * #sendIslandSummaryAdmin}), for the Admin Island Manager's detail screen. Field count is past
 * {@link PacketCodec#tuple}'s 6-argument limit (like {@link IslandSnapshotS2C}), so {@link #CODEC}
 * is hand-written with {@link PacketCodec#of}.
 *
 * <p>Wire field order: {@code islandId}, {@code ownerUuid}, {@code ownerName} (resolved via the
 * server's user cache, falls back to the raw UUID string), {@code dimension} (the registry key's
 * ResourceLocation as a String), {@code gridX}, {@code gridZ}, {@code center}, {@code boundsMin},
 * {@code boundsMax}, {@code plotBoundsMin}, {@code plotBoundsMax}, {@code islandSize},
 * {@code maxSize} (from {@code PermissionProvider#getHighestSizeAllowed}, same computation
 * {@code AdminIslandBuilder} already uses for the list — placed right after {@code islandSize} to
 * mirror {@link AdminIslandListS2C.IslandEntry}'s adjacent {@code size}/{@code maxSize} pairing),
 * {@code plotSize}, {@code islandType} (type id), {@code homeLocation}, {@code members} (list of
 * {@link IslandSnapshotS2C.MemberEntry} — reused as-is, owner included with role
 * {@code "OWNER"}, same as {@code IslandSnapshotBuilder}), {@code state}, {@code createdAt} /
 * {@code updatedAt} (already formatted human-readable strings, {@code dd/MM/yyyy HH:mm} — same
 * formatting {@code IslandMessages} itself uses, done server-side so the client never needs its
 * own date formatting logic), {@code entities} (reuses {@link IslandSnapshotS2C.EntityCounts}).
 */
public record AdminIslandDetailS2C(
		UUID islandId,
		UUID ownerUuid,
		String ownerName,
		String dimension,
		int gridX,
		int gridZ,
		BlockPos center,
		BlockPos boundsMin,
		BlockPos boundsMax,
		BlockPos plotBoundsMin,
		BlockPos plotBoundsMax,
		int islandSize,
		int maxSize,
		int plotSize,
		String islandType,
		BlockPos homeLocation,
		List<IslandSnapshotS2C.MemberEntry> members,
		String state,
		String createdAt,
		String updatedAt,
		IslandSnapshotS2C.EntityCounts entities
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<AdminIslandDetailS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_ISLAND_DETAIL_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<IslandSnapshotS2C.MemberEntry>> MEMBER_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, IslandSnapshotS2C.MemberEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminIslandDetailS2C> CODEC = StreamCodec.of(
			(buf, value) -> {
				UUIDUtil.STREAM_CODEC.encode(buf, value.islandId());
				UUIDUtil.STREAM_CODEC.encode(buf, value.ownerUuid());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.ownerName());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.dimension());
				ByteBufCodecs.VAR_INT.encode(buf, value.gridX());
				ByteBufCodecs.VAR_INT.encode(buf, value.gridZ());
				BlockPos.STREAM_CODEC.encode(buf, value.center());
				BlockPos.STREAM_CODEC.encode(buf, value.boundsMin());
				BlockPos.STREAM_CODEC.encode(buf, value.boundsMax());
				BlockPos.STREAM_CODEC.encode(buf, value.plotBoundsMin());
				BlockPos.STREAM_CODEC.encode(buf, value.plotBoundsMax());
				ByteBufCodecs.VAR_INT.encode(buf, value.islandSize());
				ByteBufCodecs.VAR_INT.encode(buf, value.maxSize());
				ByteBufCodecs.VAR_INT.encode(buf, value.plotSize());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.islandType());
				BlockPos.STREAM_CODEC.encode(buf, value.homeLocation());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.state());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.createdAt());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.updatedAt());
				IslandSnapshotS2C.EntityCounts.CODEC.encode(buf, value.entities());
			},
			buf -> new AdminIslandDetailS2C(
					UUIDUtil.STREAM_CODEC.decode(buf),
					UUIDUtil.STREAM_CODEC.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					BlockPos.STREAM_CODEC.decode(buf),
					BlockPos.STREAM_CODEC.decode(buf),
					BlockPos.STREAM_CODEC.decode(buf),
					BlockPos.STREAM_CODEC.decode(buf),
					BlockPos.STREAM_CODEC.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					BlockPos.STREAM_CODEC.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					IslandSnapshotS2C.EntityCounts.CODEC.decode(buf)
			)
	);

	@Override
	public CustomPacketPayload.Type<AdminIslandDetailS2C> type() {
		return TYPE;
	}
}
