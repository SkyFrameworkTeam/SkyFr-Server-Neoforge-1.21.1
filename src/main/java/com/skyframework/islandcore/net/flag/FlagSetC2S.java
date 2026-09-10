package com.skyframework.islandcore.net.flag;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Wire field order: flagId, value (raw string, parsed server-side the same way "/island flags
// set" parses its own "value" argument: TriState.valueOf(value.toUpperCase()) — allow/deny/default).
public record FlagSetC2S(String flagId, String value) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<FlagSetC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.FLAG_SET_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, FlagSetC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, FlagSetC2S::flagId,
			ByteBufCodecs.STRING_UTF8, FlagSetC2S::value,
			FlagSetC2S::new
	);

	@Override
	public CustomPacketPayload.Type<FlagSetC2S> type() {
		return TYPE;
	}
}
