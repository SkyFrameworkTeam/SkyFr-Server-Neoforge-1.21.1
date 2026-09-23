package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;

// One packet for all teleport actions rather than one empty payload per type, since they share the
// exact same "dispatch to TeleportManager" shape. type dispatches to
// TeleportManager#requestHome/requestSpawn/requestRtp/requestDimensionTeleport respectively; the
// SPAWN branch additionally replicates the enabled-check SpawnCommand already does before calling
// TeleportManager (see ActionReason#SPAWN_DISABLED), since that check currently lives in the text
// command, not in TeleportManagerImpl itself — DIMENSION does the same for FARMING_DISABLED when
// dimensionId happens to be FarmingConfig's own target (see ServerPacketHandlers#dispatchTeleportRequest).
//
// dimensionId is only ever present (and only ever read) when type == DIMENSION: the fixed FARMING
// type from the old single hardcoded farming button was removed (Sprint "teletransportes
// dinámicos") in favor of this generic one — the client now sends back the same Identifier string
// TeleportStatusS2C.DimensionTeleportEntry#id gave it, for whichever dynamic-section button was
// clicked, farming's own entry included.
//
// No progress/countdown packet accompanies this one for HOME/SPAWN/DIMENSION's warmup: those
// already message the player directly (in chat) at the start of the warmup, once per second during
// it, and on completion/cancellation — TeleportManagerImpl's existing tickAll() loop. Adding a
// structured progress S2C is left for a future sprint if a GUI screen actually needs to render a
// numeric countdown independent of chat text; this immediate ActionResultS2C reply is enough to
// tell the client the warmup started (or why it didn't).
public record TeleportRequestC2S(Type action, Optional<String> dimensionId) implements CustomPacketPayload {

	public enum Type {
		HOME, SPAWN, RTP, DIMENSION, OVERWORLD
	}

	public static final CustomPacketPayload.Type<TeleportRequestC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.TELEPORT_REQUEST_C2S);

	private static final StreamCodec<ByteBuf, Type> TYPE_CODEC = ByteBufCodecs.STRING_UTF8.map(Type::valueOf, Enum::name);
	private static final StreamCodec<ByteBuf, Optional<String>> DIMENSION_ID_CODEC = ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8);

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestC2S> CODEC = StreamCodec.composite(
			TYPE_CODEC, TeleportRequestC2S::action,
			DIMENSION_ID_CODEC, TeleportRequestC2S::dimensionId,
			TeleportRequestC2S::new
	);

	@Override
	public CustomPacketPayload.Type<TeleportRequestC2S> type() {
		return TYPE;
	}
}
