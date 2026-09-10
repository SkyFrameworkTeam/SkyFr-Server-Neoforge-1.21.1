package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The full vanilla-reset queue ({@code pending_vanilla_reset.json}), same entries
 * "/dimension vanilla list" prints. Wire field order: {@code queue} (list of
 * {@link QueueEntry}).
 */
public record VanillaResetListS2C(List<QueueEntry> queue) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<VanillaResetListS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.VANILLA_RESET_LIST_S2C);

	private static final StreamCodec<RegistryFriendlyByteBuf, List<QueueEntry>> QUEUE_CODEC =
			ByteBufCodecs.collection(ArrayList::new, QueueEntry.CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, VanillaResetListS2C> CODEC = StreamCodec.composite(
			QUEUE_CODEC, VanillaResetListS2C::queue,
			VanillaResetListS2C::new
	);

	@Override
	public CustomPacketPayload.Type<VanillaResetListS2C> type() {
		return TYPE;
	}

	/**
	 * Wire field order: {@code dimensionKey} ({@code "overworld"}/{@code "nether"}/{@code "end"}),
	 * {@code seed} (empty meaning "keeps the dimension's current seed"), {@code seedMode} (
	 * {@link com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset.SeedMode} name —
	 * {@code "RANDOM"}/{@code "KEEP"}/{@code "CUSTOM"}, persisted verbatim from how the seed was
	 * actually decided at confirm time rather than re-derived from whether {@code seed} is present,
	 * since a resolved RANDOM seed and a CUSTOM one are otherwise indistinguishable once resolved),
	 * {@code requestedBy}, {@code status} ({@link com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset.Status}
	 * name — always {@code "IN_PROGRESS"} today, since that's the only status ever persisted to
	 * the queue file, but sent as-is rather than hardcoded client-side in case that changes).
	 */
	public record QueueEntry(String dimensionKey, Optional<Long> seed, String seedMode, UUID requestedBy, String status) {
		private static final StreamCodec<ByteBuf, Optional<Long>> SEED_CODEC = ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG);

		public static final StreamCodec<RegistryFriendlyByteBuf, QueueEntry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, QueueEntry::dimensionKey,
				SEED_CODEC, QueueEntry::seed,
				ByteBufCodecs.STRING_UTF8, QueueEntry::seedMode,
				UUIDUtil.STREAM_CODEC, QueueEntry::requestedBy,
				ByteBufCodecs.STRING_UTF8, QueueEntry::status,
				QueueEntry::new
		);
	}
}
