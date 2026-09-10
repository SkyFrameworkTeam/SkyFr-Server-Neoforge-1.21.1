package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Every dimension {@code DimensionRegistry} manages, same fields "/dimension list" prints per
 * line. Wire field order: {@code dimensions} (list of {@link DimensionEntry}).
 */
public record DimensionListS2C(List<DimensionEntry> dimensions) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DimensionListS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_LIST_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<DimensionEntry>> DIMENSION_LIST_CODEC =
			ByteBufCodecs.collection(ArrayList::new, DimensionEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionListS2C> CODEC = StreamCodec.composite(
			DIMENSION_LIST_CODEC, DimensionListS2C::dimensions,
			DimensionListS2C::new
	);

	@Override
	public CustomPacketPayload.Type<DimensionListS2C> type() {
		return TYPE;
	}

	/**
	 * Wire field order: {@code id} (full ResourceLocation as a String, e.g. {@code "islandcore:foo"}),
	 * {@code displayName}, {@code style} ({@link com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle}
	 * name), {@code seed}, {@code state} ({@link com.skyframework.islandcore.dimension.model.DimensionState} name).
	 */
	public record DimensionEntry(String id, String displayName, String style, long seed, String state) {
		public static final StreamCodec<RegistryFriendlyByteBuf, DimensionEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, DimensionEntry::id,
				ByteBufCodecs.STRING_UTF8, DimensionEntry::displayName,
				ByteBufCodecs.STRING_UTF8, DimensionEntry::style,
				ByteBufCodecs.VAR_LONG, DimensionEntry::seed,
				ByteBufCodecs.STRING_UTF8, DimensionEntry::state,
				DimensionEntry::new
		);
	}
}
