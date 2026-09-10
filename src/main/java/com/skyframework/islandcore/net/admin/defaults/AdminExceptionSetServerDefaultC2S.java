package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Sets the server-wide default preset for one exception group ("/island admin exceptions
// set-default <group> <preset>" network equivalent). Wire field order: groupId, preset
// ("nadie"/"miembros"/"aliados"/"todos"). Operator-only.
public record AdminExceptionSetServerDefaultC2S(String groupId, String preset) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AdminExceptionSetServerDefaultC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_EXCEPTION_SET_SERVER_DEFAULT_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminExceptionSetServerDefaultC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AdminExceptionSetServerDefaultC2S::groupId,
			ByteBufCodecs.STRING_UTF8, AdminExceptionSetServerDefaultC2S::preset,
			AdminExceptionSetServerDefaultC2S::new
	);

	@Override
	public CustomPacketPayload.Type<AdminExceptionSetServerDefaultC2S> type() {
		return TYPE;
	}
}
