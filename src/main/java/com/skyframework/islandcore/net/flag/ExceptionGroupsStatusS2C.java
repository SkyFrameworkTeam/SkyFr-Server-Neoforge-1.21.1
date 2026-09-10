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
 * {@code ActionResultS2C.fail(ActionReason.NO_ISLAND)} instead, no empty/placeholder snapshot.
 * Wire field order: {@code groups} (list of {@link GroupEntry}, in
 * {@code ExceptionGroupRegistry.getAllGroups()}'s registration order).
 *
 * <p>Exception groups now resolve per role exactly like ROLE_BASED flags (see ExceptionResolver) —
 * this record mirrors {@link FlagsStatusS2C.FlagEntry}'s shape instead of the old single
 * {@code enabled} boolean.
 */
public record ExceptionGroupsStatusS2C(List<GroupEntry> groups) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ExceptionGroupsStatusS2C> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.EXCEPTION_GROUPS_STATUS_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<GroupEntry>> GROUP_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, GroupEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, ExceptionGroupsStatusS2C> CODEC = StreamCodec.composite(
			GROUP_LIST_CODEC, ExceptionGroupsStatusS2C::groups,
			ExceptionGroupsStatusS2C::new
	);

	@Override
	public CustomPacketPayload.Type<ExceptionGroupsStatusS2C> type() {
		return TYPE;
	}

	// Wire field order: groupId, category ("BLOCK" or "ENTITY", ExceptionGroupCategory#name()),
	// resolvedByRole (one FlagsStatusS2C.RoleValueEntry per IslandRole, reusing that record exactly
	// — same shape, no reason to duplicate it), currentPreset ("nadie"/"miembros"/"aliados"/"todos"
	// if the current VISITOR/ALLY/MEMBER combination exactly matches one of those presets,
	// or "custom" if not — see ExceptionResolver#currentPreset), ownerConfigurable
	// (ExceptionGroup#isOwnerConfigurable()).
	public record GroupEntry(
			String groupId, String category, List<FlagsStatusS2C.RoleValueEntry> resolvedByRole, String currentPreset, boolean ownerConfigurable
	) {
		private static final StreamCodec<RegistryFriendlyByteBuf, List<FlagsStatusS2C.RoleValueEntry>> ROLE_VALUE_LIST_CODEC =
				ByteBufCodecs.collection(ArrayList::new, FlagsStatusS2C.RoleValueEntry.CODEC);

		public static final StreamCodec<RegistryFriendlyByteBuf, GroupEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, GroupEntry::groupId,
				ByteBufCodecs.STRING_UTF8, GroupEntry::category,
				ROLE_VALUE_LIST_CODEC, GroupEntry::resolvedByRole,
				ByteBufCodecs.STRING_UTF8, GroupEntry::currentPreset,
				ByteBufCodecs.BOOL, GroupEntry::ownerConfigurable,
				GroupEntry::new
		);
	}
}
