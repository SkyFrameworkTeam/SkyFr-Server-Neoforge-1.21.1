package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent only when the requesting player has an island — if not, the server sends
 * {@code ActionResultS2C.fail(ActionReason.NO_ISLAND)} instead (see FlagsStatusRequestC2S's
 * handler), no empty/placeholder snapshot. Wire field order: {@code flags} (list of
 * {@link FlagEntry}, in {@code FlagRegistry.all()}'s registration order — construccion, interact,
 * containers, entities, redstone, fire_spread, pvp_damage, mob_damage).
 */
public record FlagsStatusS2C(List<FlagEntry> flags) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<FlagsStatusS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.FLAGS_STATUS_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<FlagEntry>> FLAG_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, FlagEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, FlagsStatusS2C> CODEC = StreamCodec.composite(
			FLAG_LIST_CODEC, FlagsStatusS2C::flags,
			FlagsStatusS2C::new
	);

	@Override
	public CustomPacketPayload.Type<FlagsStatusS2C> type() {
		return TYPE;
	}

	/**
	 * Wire field order: {@code flagId}, {@code category} ({@code "ROLE_BASED"} or
	 * {@code "ISLAND_GLOBAL"}, {@link com.skyframework.islandcore.protection.flag.FlagCategory#name()}),
	 * {@code resolvedValue} (ISLAND_GLOBAL only — {@code FlagResolver#resolveGlobal}; always
	 * {@code false} and meaningless for a ROLE_BASED entry, read {@code resolvedByRole} instead),
	 * {@code resolvedByRole} (ROLE_BASED only — one {@link RoleValueEntry} per
	 * {@code IslandRole}, exactly what "/island flags" itself computes via
	 * {@code FlagResolver#resolveForRole}; always empty for an ISLAND_GLOBAL entry),
	 * {@code islandOverride} (this island's own override for the flag, {@link
	 * com.skyframework.islandcore.protection.flag.TriState#name()} — {@code "DEFAULT"} if never
	 * touched; for ROLE_BASED this is a single value since {@code /island flags set} always
	 * overrides every role uniformly, see {@code IslandData#setRoleFlagOverrideForAllRoles}),
	 * {@code currentPreset} (ROLE_BASED only, added after {@code islandOverride} — the
	 * {@link com.skyframework.islandcore.protection.flag.FlagPreset#getId()} whose VISITOR/ALLY/
	 * MEMBER combination exactly matches this flag's current {@code resolvedByRole} values,
	 * or {@code "custom"} if none match; always {@code ""} — not applicable — for an ISLAND_GLOBAL
	 * entry), {@code missingRequiredPermission} ({@code true} only if
	 * {@code FlagPermissionRequirements} has a node set for this flag AND the requesting player
	 * (the island owner — this payload is only ever built for them) lacks it; presentation only,
	 * the real gate stays server-side in {@code IslandActionService#updateFlag}/{@code
	 * applyFlagPreset}).
	 */
	public record FlagEntry(
			String flagId,
			String category,
			boolean resolvedValue,
			List<RoleValueEntry> resolvedByRole,
			String islandOverride,
			String currentPreset,
			boolean missingRequiredPermission
	) {
		private static final StreamCodec<RegistryFriendlyByteBuf, List<RoleValueEntry>> ROLE_VALUE_LIST_CODEC =
				ByteBufCodecs.collection(ArrayList::new, RoleValueEntry.CODEC);

		// 7 fields is past PacketCodec#tuple's 6-argument limit (like AdminIslandDetailS2C), so this
		// is hand-written with PacketCodec#of instead.
		public static final StreamCodec<RegistryFriendlyByteBuf, FlagEntry> CODEC = StreamCodec.of(
				(buf, value) -> {
					ByteBufCodecs.STRING_UTF8.encode(buf, value.flagId());
					ByteBufCodecs.STRING_UTF8.encode(buf, value.category());
					ByteBufCodecs.BOOL.encode(buf, value.resolvedValue());
					ROLE_VALUE_LIST_CODEC.encode(buf, value.resolvedByRole());
					ByteBufCodecs.STRING_UTF8.encode(buf, value.islandOverride());
					ByteBufCodecs.STRING_UTF8.encode(buf, value.currentPreset());
					ByteBufCodecs.BOOL.encode(buf, value.missingRequiredPermission());
				},
				buf -> new FlagEntry(
						ByteBufCodecs.STRING_UTF8.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf),
						ByteBufCodecs.BOOL.decode(buf),
						ROLE_VALUE_LIST_CODEC.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf),
						ByteBufCodecs.STRING_UTF8.decode(buf),
						ByteBufCodecs.BOOL.decode(buf)
				)
		);
	}

	// Wire field order: role (IslandRole#name()), value (TriState#name() — always ALLOW or DENY,
	// FlagResolver#resolveForRole never returns DEFAULT).
	public record RoleValueEntry(String role, String value) {
		public static final StreamCodec<RegistryFriendlyByteBuf, RoleValueEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, RoleValueEntry::role,
				ByteBufCodecs.STRING_UTF8, RoleValueEntry::value,
				RoleValueEntry::new
		);
	}
}
