package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// biomeId is the raw ResourceLocation string (e.g. "minecraft:jungle"), parsed server-side with
// ResourceLocation.of(...) the same way IdentifierArgumentType already hands one to executeBiome — a
// malformed string is treated as ActionReason.BIOME_NOT_FOUND rather than crashing the handler.
public record IslandBiomeChangeC2S(String biomeId) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<IslandBiomeChangeC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ISLAND_BIOME_CHANGE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, IslandBiomeChangeC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, IslandBiomeChangeC2S::biomeId,
			IslandBiomeChangeC2S::new
	);

	@Override
	public CustomPacketPayload.Type<IslandBiomeChangeC2S> type() {
		return TYPE;
	}
}
