package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Replaces the old boolean-shaped ExceptionGroupSetC2S — exception groups now resolve per role,
// exact mirror of FlagSetPresetC2S. Wire field order: groupId, preset ("nadie"/"miembros"/
// "aliados"/"todos"). Only valid for owner-configurable groups.
public record ExceptionGroupSetPresetC2S(String groupId, String preset) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ExceptionGroupSetPresetC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.EXCEPTION_GROUP_SET_PRESET_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, ExceptionGroupSetPresetC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, ExceptionGroupSetPresetC2S::groupId,
			ByteBufCodecs.STRING_UTF8, ExceptionGroupSetPresetC2S::preset,
			ExceptionGroupSetPresetC2S::new
	);

	@Override
	public CustomPacketPayload.Type<ExceptionGroupSetPresetC2S> type() {
		return TYPE;
	}
}
