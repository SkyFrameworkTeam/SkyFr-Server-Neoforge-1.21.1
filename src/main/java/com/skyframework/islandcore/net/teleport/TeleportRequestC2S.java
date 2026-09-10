package com.skyframework.islandcore.net.teleport;

import com.skyframework.islandcore.net.NetworkChannels;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// One packet for all four teleport actions rather than four empty payloads, since they share the
// exact same "no extra data, dispatch to TeleportManager" shape. action dispatches to
// TeleportManager#requestHome/requestSpawn/requestRtp/requestFarming respectively; the SPAWN/
// FARMING branches additionally replicate the enabled-check SpawnCommand/FarmingCommand already
// do before calling TeleportManager (see ActionReason#SPAWN_DISABLED/FARMING_DISABLED), since
// that check currently lives in the text command, not in TeleportManagerImpl itself.
//
// No progress/countdown packet accompanies this one for HOME/SPAWN/FARMING's warmup: those three
// already message the player directly (in chat) at the start of the warmup, once per second during
// it, and on completion/cancellation — TeleportManagerImpl's existing tickAll() loop. Adding a
// structured progress S2C is left for a future sprint if a GUI screen actually needs to render a
// numeric countdown independent of chat text; this immediate ActionResultS2C reply is enough to
// tell the client the warmup started (or why it didn't).
// The record component is named "action" (not "type") because CustomPacketPayload's own abstract
// method is itself called type() — Fabric's equivalent CustomPayload#getId() didn't collide with a
// same-named record component the way NeoForge/Mojmap's type() does, so this field had to be
// renamed on this port (same situation as IslandSnapshotS2C#islandType).
public record TeleportRequestC2S(Type action) implements CustomPacketPayload {

	public enum Type {
		HOME, SPAWN, RTP, FARMING
	}

	public static final CustomPacketPayload.Type<TeleportRequestC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.TELEPORT_REQUEST_C2S);

	private static final StreamCodec<ByteBuf, Type> ACTION_CODEC = ByteBufCodecs.STRING_UTF8.map(Type::valueOf, Enum::name);

	public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRequestC2S> CODEC = StreamCodec.composite(
			ACTION_CODEC, TeleportRequestC2S::action,
			TeleportRequestC2S::new
	);

	@Override
	public CustomPacketPayload.Type<TeleportRequestC2S> type() {
		return TYPE;
	}
}
