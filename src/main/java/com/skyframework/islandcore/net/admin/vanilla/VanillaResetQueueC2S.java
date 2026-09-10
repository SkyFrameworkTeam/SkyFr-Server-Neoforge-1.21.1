package com.skyframework.islandcore.net.admin.vanilla;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

/**
 * Network equivalent of "/dimension vanilla regenerate &lt;dimensionKey&gt; [seed]" (the initial
 * request, not the confirm) — calls the same {@code VanillaResetService#requestReset}. Wire field
 * order: {@code dimension} (one of {@code VanillaResetService.VALID_DIMENSION_KEYS}:
 * {@code "overworld"}/{@code "nether"}/{@code "end"}), {@code seedMode} (either
 * {@code "CUSTOM"} or {@code "DEFAULT"} — {@code requestReset} itself only ever takes a nullable
 * explicit seed, it has no such mode concept, so this is the network equivalent of "did the text
 * command's optional seed argument get supplied or not": {@code "CUSTOM"} maps to
 * {@code explicitSeed = seedValue}, anything else maps to {@code explicitSeed = null}, deferring
 * to the server's own {@code VanillaResetConfig} seed-mode default exactly like omitting the
 * argument does today), {@code seedValue} (only meaningful when {@code seedMode == "CUSTOM"}).
 */
public record VanillaResetQueueC2S(String dimension, String seedMode, Optional<Long> seedValue) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<VanillaResetQueueC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.VANILLA_RESET_QUEUE_C2S);

	private static final StreamCodec<ByteBuf, Optional<Long>> SEED_CODEC = ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG);

	public static final StreamCodec<RegistryFriendlyByteBuf, VanillaResetQueueC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, VanillaResetQueueC2S::dimension,
			ByteBufCodecs.STRING_UTF8, VanillaResetQueueC2S::seedMode,
			SEED_CODEC, VanillaResetQueueC2S::seedValue,
			VanillaResetQueueC2S::new
	);

	@Override
	public CustomPacketPayload.Type<VanillaResetQueueC2S> type() {
		return TYPE;
	}
}
