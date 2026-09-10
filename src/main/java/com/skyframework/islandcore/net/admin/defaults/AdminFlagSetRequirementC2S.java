package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Sets (or clears) the LuckPerms node required to change one flag ("/island admin flags require
// <flag> <nodo|ninguno>" network equivalent) — see FlagPermissionRequirements's class javadoc.
// Wire field order: flagId, permissionNode. An empty string for permissionNode clears the
// requirement (mirrors "ninguno" on the text-command path); any other value sets/replaces it.
// Operator-only. Unlike AdminFlagSetServerDefaultC2S, this reaches both ROLE_BASED and
// ISLAND_GLOBAL flags, since the requirement gate applies to any flag a player can change.
public record AdminFlagSetRequirementC2S(String flagId, String permissionNode) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AdminFlagSetRequirementC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_FLAG_SET_REQUIREMENT_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminFlagSetRequirementC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AdminFlagSetRequirementC2S::flagId,
			ByteBufCodecs.STRING_UTF8, AdminFlagSetRequirementC2S::permissionNode,
			AdminFlagSetRequirementC2S::new
	);

	@Override
	public CustomPacketPayload.Type<AdminFlagSetRequirementC2S> type() {
		return TYPE;
	}
}
