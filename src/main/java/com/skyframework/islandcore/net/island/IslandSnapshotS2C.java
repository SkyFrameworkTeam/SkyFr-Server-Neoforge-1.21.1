package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Full state of the receiving player's own island, or {@code exists = false} with default/empty
 * values in every other field if they don't have one yet. Built by {@link IslandSnapshotBuilder}
 * from the existing island services — this record only carries data, no business logic.
 *
 * <p>Field count (13) is past {@link StreamCodec#composite}'s argument limit, so {@link #CODEC} is
 * written by hand with {@link StreamCodec#of}; the nested per-entry records below stay within
 * the limit and keep their own small composite codecs.
 *
 * <p>The island-type field is named {@code islandType} here (not {@code type}) because
 * {@link CustomPacketPayload}'s own abstract method is itself called {@code type()} — Fabric's
 * equivalent {@code CustomPayload#getId()} didn't collide with a same-named record component the
 * way NeoForge/Mojmap's {@code type()} does, so this one field had to be renamed on this port.
 *
 * <p><b>Wire format changed:</b> {@code biomeCooldownRemainingSeconds} was inserted after
 * {@code currentBiomeId} (grouped with the other biome field), and {@code incomingInvite} was
 * inserted after {@code pendingInvites} (grouped with the other invite field) — this is a
 * client/server protocol break for IslandCoreClient's own copy of this record, which needs the
 * matching update made separately in that project.
 */
public record IslandSnapshotS2C(
		boolean exists,
		int size,
		int maxSize,
		String islandType,
		String currentBiomeId,
		int biomeCooldownRemainingSeconds,
		Optional<BlockPos> home,
		String state,
		List<MemberEntry> members,
		List<PendingInviteEntry> pendingInvites,
		Optional<IncomingInviteEntry> incomingInvite,
		List<SettingEntry> settings,
		EntityCounts entities
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<IslandSnapshotS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.ISLAND_SNAPSHOT_S2C);

	// ByteBufCodecs.optional(StreamCodec<B, V>) requires an exact B match (no "? super B" wildcard
	// like collection() below has), so this stays typed over plain ByteBuf rather than
	// RegistryFriendlyByteBuf; encode/decode calls below still accept a RegistryFriendlyByteBuf argument fine.
	private static final StreamCodec<ByteBuf, Optional<BlockPos>> HOME_CODEC = ByteBufCodecs.optional(BlockPos.STREAM_CODEC);
	// IncomingInviteEntry.CODEC is already typed over RegistryFriendlyByteBuf (like every other nested
	// entry here), so unlike HOME_CODEC above this needs no ByteBuf/RegistryFriendlyByteBuf split.
	private static final StreamCodec<RegistryFriendlyByteBuf, Optional<IncomingInviteEntry>> INCOMING_INVITE_CODEC =
			ByteBufCodecs.optional(IncomingInviteEntry.CODEC);
	private static final StreamCodec<RegistryFriendlyByteBuf, List<MemberEntry>> MEMBER_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, MemberEntry.CODEC);
	private static final StreamCodec<RegistryFriendlyByteBuf, List<PendingInviteEntry>> PENDING_INVITE_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, PendingInviteEntry.CODEC);
	private static final StreamCodec<RegistryFriendlyByteBuf, List<SettingEntry>> SETTING_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, SettingEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, IslandSnapshotS2C> CODEC = StreamCodec.of(
			(buf, value) -> {
				ByteBufCodecs.BOOL.encode(buf, value.exists());
				ByteBufCodecs.VAR_INT.encode(buf, value.size());
				ByteBufCodecs.VAR_INT.encode(buf, value.maxSize());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.islandType());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.currentBiomeId());
				ByteBufCodecs.VAR_INT.encode(buf, value.biomeCooldownRemainingSeconds());
				HOME_CODEC.encode(buf, value.home());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.state());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				PENDING_INVITE_LIST_CODEC.encode(buf, value.pendingInvites());
				INCOMING_INVITE_CODEC.encode(buf, value.incomingInvite());
				SETTING_LIST_CODEC.encode(buf, value.settings());
				EntityCounts.CODEC.encode(buf, value.entities());
			},
			buf -> new IslandSnapshotS2C(
					ByteBufCodecs.BOOL.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.VAR_INT.decode(buf),
					HOME_CODEC.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					PENDING_INVITE_LIST_CODEC.decode(buf),
					INCOMING_INVITE_CODEC.decode(buf),
					SETTING_LIST_CODEC.decode(buf),
					EntityCounts.CODEC.decode(buf)
			)
	);

	@Override
	public CustomPacketPayload.Type<IslandSnapshotS2C> type() {
		return TYPE;
	}

	public record MemberEntry(UUID uuid, String name, String role) {
		public static final StreamCodec<RegistryFriendlyByteBuf, MemberEntry> CODEC = StreamCodec.composite(
				net.minecraft.core.UUIDUtil.STREAM_CODEC, MemberEntry::uuid,
				ByteBufCodecs.STRING_UTF8, MemberEntry::name,
				ByteBufCodecs.STRING_UTF8, MemberEntry::role,
				MemberEntry::new
		);
	}

	public record PendingInviteEntry(String targetName, int expiresInSeconds) {
		public static final StreamCodec<RegistryFriendlyByteBuf, PendingInviteEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, PendingInviteEntry::targetName,
				ByteBufCodecs.VAR_INT, PendingInviteEntry::expiresInSeconds,
				PendingInviteEntry::new
		);
	}

	// An invite where the receiving player is the INVITEE, not the island's owner (contrast
	// PendingInviteEntry above, which lists invites the player's own island sent out). Empty
	// (IslandSnapshotS2C#incomingInvite) means no pending incoming invite, or it already expired —
	// InviteManager#getPendingInvite already filters that server-side.
	public record IncomingInviteEntry(String inviterName, int expiresInSeconds) {
		public static final StreamCodec<RegistryFriendlyByteBuf, IncomingInviteEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, IncomingInviteEntry::inviterName,
				ByteBufCodecs.VAR_INT, IncomingInviteEntry::expiresInSeconds,
				IncomingInviteEntry::new
		);
	}

	public record SettingEntry(String key, boolean value) {
		public static final StreamCodec<RegistryFriendlyByteBuf, SettingEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, SettingEntry::key,
				ByteBufCodecs.BOOL, SettingEntry::value,
				SettingEntry::new
		);
	}

	public record EntityCounts(int players, int hostile, int passive, int cobblemon, int items, int other) {
		public static final StreamCodec<RegistryFriendlyByteBuf, EntityCounts> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, EntityCounts::players,
				ByteBufCodecs.VAR_INT, EntityCounts::hostile,
				ByteBufCodecs.VAR_INT, EntityCounts::passive,
				ByteBufCodecs.VAR_INT, EntityCounts::cobblemon,
				ByteBufCodecs.VAR_INT, EntityCounts::items,
				ByteBufCodecs.VAR_INT, EntityCounts::other,
				EntityCounts::new
		);

		public static final EntityCounts EMPTY = new EntityCounts(0, 0, 0, 0, 0, 0);
	}
}
