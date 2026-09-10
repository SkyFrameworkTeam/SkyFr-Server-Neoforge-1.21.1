package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Wire field order: flagId, preset ("nadie"/"miembros"/"aliados"/"todos" —
// see protection.flag.FlagPreset). Only valid for ROLE_BASED flags; the server validates both and
// reports ActionReason.INVALID_FLAG_PRESET for either mismatch — see IslandActionService#applyFlagPreset.
public record FlagSetPresetC2S(String flagId, String preset) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FlagSetPresetC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.FLAG_SET_PRESET_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, FlagSetPresetC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, FlagSetPresetC2S::flagId,
			ByteBufCodecs.STRING_UTF8, FlagSetPresetC2S::preset,
			FlagSetPresetC2S::new
	);

	@Override
	public CustomPacketPayload.Type<FlagSetPresetC2S> type() {
		return TYPE;
	}
}
