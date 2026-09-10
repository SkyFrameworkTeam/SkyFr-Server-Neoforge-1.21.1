package com.skyframework.islandcore.net.biome;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Every configured biome tier (built by BiomeTiersBuilder from BiomeTierRegistry#getAllTiers,
// reusing #canUse per tier for the "unlocked" flag), so the client can render locked tiers too
// (e.g. "unlocks with permission X") instead of only ever seeing biomes it already has access to.
public record BiomeTiersS2C(List<TierEntry> tiers) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<BiomeTiersS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.BIOME_TIERS_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<TierEntry>> TIERS_CODEC =
			ByteBufCodecs.collection(ArrayList::new, TierEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, BiomeTiersS2C> CODEC = StreamCodec.composite(
			TIERS_CODEC, BiomeTiersS2C::tiers,
			BiomeTiersS2C::new
	);

	@Override
	public CustomPacketPayload.Type<BiomeTiersS2C> type() {
		return TYPE;
	}

	// permissionRequired is empty for a base tier (BiomeTier#permission() == null), matching
	// BiomeTierRegistry's own "null permission == usable by anyone" convention.
	public record TierEntry(String tierId, Optional<String> permissionRequired, boolean unlocked, List<BiomeEntry> biomes) {

		private static final StreamCodec<ByteBuf, Optional<String>> PERMISSION_CODEC = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8);
		private static final StreamCodec<RegistryFriendlyByteBuf, List<BiomeEntry>> BIOMES_CODEC =
				ByteBufCodecs.collection(ArrayList::new, BiomeEntry.CODEC);

		public static final StreamCodec<RegistryFriendlyByteBuf, TierEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, TierEntry::tierId,
				PERMISSION_CODEC, TierEntry::permissionRequired,
				ByteBufCodecs.BOOL, TierEntry::unlocked,
				BIOMES_CODEC, TierEntry::biomes,
				TierEntry::new
		);
	}

	// label is a display-friendly derivation of the biome's ResourceLocation path (e.g.
	// "minecraft:dark_forest" -> "Dark Forest") computed once server-side by BiomeTiersBuilder, so
	// every client renders the same text without each one reimplementing the same formatting.
	public record BiomeEntry(String biomeId, String label) {
		public static final StreamCodec<RegistryFriendlyByteBuf, BiomeEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, BiomeEntry::biomeId,
				ByteBufCodecs.STRING_UTF8, BiomeEntry::label,
				BiomeEntry::new
		);
	}
}
