package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

// Network equivalent of "/dimension create <id> <displayName> <style> [seed]". id is the path
// only (see DimensionDetailRequestC2S); style is DimensionGeneratorStyle's name (case-insensitive
// on the handler side, matching DimensionCommand#executeCreate's own .toUpperCase(Locale.ROOT)).
// seed absent means "random", exactly like the text command omitting its optional seed argument.
public record DimensionCreateC2S(String id, String displayName, String style, Optional<Long> seed) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DimensionCreateC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_CREATE_C2S);

	private static final StreamCodec<ByteBuf, Optional<Long>> SEED_CODEC = ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG);

	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionCreateC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, DimensionCreateC2S::id,
			ByteBufCodecs.STRING_UTF8, DimensionCreateC2S::displayName,
			ByteBufCodecs.STRING_UTF8, DimensionCreateC2S::style,
			SEED_CODEC, DimensionCreateC2S::seed,
			DimensionCreateC2S::new
	);

	@Override
	public CustomPacketPayload.Type<DimensionCreateC2S> type() {
		return TYPE;
	}
}
