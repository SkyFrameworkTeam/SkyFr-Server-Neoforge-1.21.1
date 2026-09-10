package com.skyframework.islandcore.net.admin.dimension;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Same fields "/dimension info &lt;id&gt;" prints. Wire field order: {@code id} (full ResourceLocation
 * as a String), {@code displayName}, {@code style}, {@code seed}, {@code state},
 * {@code createdAt} / {@code updatedAt} (already formatted human-readable strings, same
 * formatting as {@link com.skyframework.islandcore.net.admin.island.AdminIslandDetailS2C}).
 *
 * <p>Sent only on success — a target not found or a not-yet-decided style/id error replies with
 * {@code ActionResultS2C.fail(...)} instead (see {@code ActionReason#DIMENSION_NOT_FOUND}).
 */
public record DimensionDetailS2C(
		String id,
		String displayName,
		String style,
		long seed,
		String state,
		String createdAt,
		String updatedAt
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<DimensionDetailS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.DIMENSION_DETAIL_S2C);

	// 7 fields is past PacketCodec#tuple's 6-argument limit (confirmed against the decompiled
	// PacketCodec source, same reasoning as IslandSnapshotS2C/AdminIslandDetailS2C), so this is
	// hand-written with PacketCodec#of instead.
	public static final StreamCodec<RegistryFriendlyByteBuf, DimensionDetailS2C> CODEC = StreamCodec.of(
			(buf, value) -> {
				ByteBufCodecs.STRING_UTF8.encode(buf, value.id());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.displayName());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.style());
				ByteBufCodecs.VAR_LONG.encode(buf, value.seed());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.state());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.createdAt());
				ByteBufCodecs.STRING_UTF8.encode(buf, value.updatedAt());
			},
			buf -> new DimensionDetailS2C(
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.VAR_LONG.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf),
					ByteBufCodecs.STRING_UTF8.decode(buf)
			)
	);

	@Override
	public CustomPacketPayload.Type<DimensionDetailS2C> type() {
		return TYPE;
	}
}
