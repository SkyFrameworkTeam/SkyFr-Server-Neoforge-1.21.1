package com.skyframework.islandcore.net.admin.spawn;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * Wire field order: {@code exists}, {@code size} (0 if {@code exists} is false),
 * {@code homeLocation} (empty if {@code exists} is false). {@code exists = false} is a normal,
 * expected state (the Spawn island simply hasn't been created yet) — not an error, so this is
 * never wrapped in an {@code ActionResultS2C} failure.
 */
public record SpawnStatusS2C(boolean exists, int size, Optional<BlockPos> homeLocation) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnStatusS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.SPAWN_STATUS_S2C);

	// See IslandSnapshotS2C's HOME_CODEC comment: ByteBufCodecs.optional needs an exact ByteBuf
	// match, so this stays typed over plain ByteBuf rather than RegistryFriendlyByteBuf.
	private static final StreamCodec<ByteBuf, Optional<BlockPos>> HOME_CODEC = ByteBufCodecs.optional(BlockPos.STREAM_CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, SpawnStatusS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, SpawnStatusS2C::exists,
			ByteBufCodecs.VAR_INT, SpawnStatusS2C::size,
			HOME_CODEC, SpawnStatusS2C::homeLocation,
			SpawnStatusS2C::new
	);

	public static SpawnStatusS2C absent() {
		return new SpawnStatusS2C(false, 0, Optional.empty());
	}

	@Override
	public CustomPacketPayload.Type<SpawnStatusS2C> type() {
		return TYPE;
	}
}
