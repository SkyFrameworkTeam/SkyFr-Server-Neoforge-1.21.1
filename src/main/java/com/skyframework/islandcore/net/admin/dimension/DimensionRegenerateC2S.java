package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

// Network equivalent of "/dimension regenerate <id> [seed]": calls the same
// DimensionRegistry#requestRegeneration(id, senderUuid, newSeed) the text command uses. seed
// absent means "random", same as DimensionCreateC2S.
public record DimensionRegenerateC2S(String id, Optional<Long> seed) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DimensionRegenerateC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_REGENERATE_C2S);

	private static final StreamCodec<ByteBuf, Optional<Long>> SEED_CODEC = ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG);

	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionRegenerateC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, DimensionRegenerateC2S::id,
			SEED_CODEC, DimensionRegenerateC2S::seed,
			DimensionRegenerateC2S::new
	);

	@Override
	public CustomPacketPayload.Type<DimensionRegenerateC2S> type() {
		return TYPE;
	}
}
