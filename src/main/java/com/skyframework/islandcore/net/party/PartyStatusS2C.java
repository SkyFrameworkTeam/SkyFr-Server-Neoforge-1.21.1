package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Full state of the receiving player's own party, or {@code hasParty = false} with default/empty
 * values in every other field if they don't have one — {@code hasParty = false} is a normal,
 * valid state (not an error), same as {@code IslandSnapshotS2C#exists}.
 *
 * <p>Field count (8) is past {@link StreamCodec#tuple}'s 6-argument limit, so {@link #CODEC} is
 * written by hand with {@link StreamCodec#of}, same as {@code IslandSnapshotS2C}.
 *
 * <p>Wire field order: {@code hasParty}, {@code partyId} (the zero UUID if {@code hasParty} is
 * false), {@code name} ({@code ""} if absent), {@code leaderUuid} (the zero UUID if absent),
 * {@code leaderName} (resolved via {@code server.getUserCache()}, {@code ""} if absent),
 * {@code members} (list of {@link MemberEntry} — every party member including the leader, same
 * set {@code PartyData#getMembers()} returns; empty if absent), {@code incomingInvite} (an invite
 * where the receiving player is the INVITEE, mirroring {@code IslandSnapshotS2C#incomingInvite};
 * empty if there is none pending, or it already expired. In practice only ever non-empty when
 * {@code hasParty} is false: {@code PartyInviteManager#requestInvite} refuses to invite a player
 * who's already in a party, so a player who currently has one can never also have a pending
 * invite).
 *
 * <p>No {@code alliedParties} field (retired along with {@code PartyData#alliedPartyIds} and
 * {@code /party ally add/remove} — see the "alianzas" consolidation sprint: individual-player
 * allies now live entirely on the island side, see {@code IslandSnapshotS2C}'s member list).
 */
public record PartyStatusS2C(
		boolean hasParty,
		UUID partyId,
		String name,
		UUID leaderUuid,
		String leaderName,
		List<MemberEntry> members,
		Optional<IncomingPartyInviteEntry> incomingInvite
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<PartyStatusS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.PARTY_STATUS_S2C);

	// Sentinel for "no party" — mirrors Island.SERVER_OWNER_UUID's own zero-UUID convention.
	public static final UUID NO_PARTY_UUID = new UUID(0, 0);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<MemberEntry>> MEMBER_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, MemberEntry.CODEC);
	private static final StreamCodec<RegistryFriendlyByteBuf, Optional<IncomingPartyInviteEntry>> INCOMING_INVITE_CODEC =
			ByteBufCodecs.optional(IncomingPartyInviteEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyStatusS2C> CODEC = StreamCodec.ofMember(
			(value, buf) -> {
				ByteBufCodecs.BOOL.encode(buf, value.hasParty());
				UUIDUtil.STREAM_CODEC.encode(buf, value.partyId());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.name());
				UUIDUtil.STREAM_CODEC.encode(buf, value.leaderUuid());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.leaderName());
				MEMBER_LIST_CODEC.encode(buf, value.members());
				INCOMING_INVITE_CODEC.encode(buf, value.incomingInvite());
			},
			buf -> new PartyStatusS2C(
					ByteBufCodecs.BOOL.decode(buf),
					UUIDUtil.STREAM_CODEC.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					UUIDUtil.STREAM_CODEC.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					MEMBER_LIST_CODEC.decode(buf),
					INCOMING_INVITE_CODEC.decode(buf)
			)
	);

	// Mirrors SpawnBuildProtectionStatusS2C#absent(): the "no party" state, as a static factory on
	// the record itself. incomingInvite is still a real parameter here (not hardcoded empty) since
	// it's the one field that CAN be populated even while hasParty is false — see PartyStatusBuilder.
	public static PartyStatusS2C absent(Optional<IncomingPartyInviteEntry> incomingInvite) {
		return new PartyStatusS2C(false, NO_PARTY_UUID, "", NO_PARTY_UUID, "", List.of(), incomingInvite);
	}

	@Override
	public CustomPacketPayload.Type<PartyStatusS2C> type() {
		return TYPE;
	}

	public record MemberEntry(UUID uuid, String name) {
		public static final StreamCodec<RegistryFriendlyByteBuf, MemberEntry> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, MemberEntry::uuid,
				ByteBufCodecs.STRING_UTF8, MemberEntry::name,
				MemberEntry::new
		);
	}

	// Wire field order: inviterName (resolved), partyName (resolved), expiresInSeconds.
	public record IncomingPartyInviteEntry(String inviterName, String partyName, int expiresInSeconds) {
		public static final StreamCodec<RegistryFriendlyByteBuf, IncomingPartyInviteEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, IncomingPartyInviteEntry::inviterName,
				ByteBufCodecs.STRING_UTF8, IncomingPartyInviteEntry::partyName,
				ByteBufCodecs.VAR_INT, IncomingPartyInviteEntry::expiresInSeconds,
				IncomingPartyInviteEntry::new
		);
	}
}
