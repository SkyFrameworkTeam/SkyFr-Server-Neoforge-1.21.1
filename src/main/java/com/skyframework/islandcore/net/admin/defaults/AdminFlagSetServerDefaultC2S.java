package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Sets the server-wide default preset for one ROLE_BASED flag ("/island admin flags set-default
// <flag> <preset>" network equivalent). Wire field order: flagId, preset ("nadie"/"miembros"/
// "aliados"/"todos"). Operator-only; ISLAND_GLOBAL flags aren't reachable through this payload (see
// AdminDefaultsStatusS2C's class javadoc).
public record AdminFlagSetServerDefaultC2S(String flagId, String preset) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AdminFlagSetServerDefaultC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_FLAG_SET_SERVER_DEFAULT_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminFlagSetServerDefaultC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AdminFlagSetServerDefaultC2S::flagId,
			ByteBufCodecs.STRING_UTF8, AdminFlagSetServerDefaultC2S::preset,
			AdminFlagSetServerDefaultC2S::new
	);

	@Override
	public CustomPacketPayload.Type<AdminFlagSetServerDefaultC2S> type() {
		return TYPE;
	}
}
