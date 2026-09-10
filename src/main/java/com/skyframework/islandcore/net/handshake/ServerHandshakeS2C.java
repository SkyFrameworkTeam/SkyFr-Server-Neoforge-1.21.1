package com.skyframework.islandcore.net.handshake;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server-side copy of IslandCoreClient's network.handshake.ServerHandshakeS2C — see the note on
// ClientHandshakeC2S about why this can't be a shared class between the two mod projects.
//
// Field order: protocolVersion, protocolCompatible, isOperator. protocolCompatible is new —
// true when the protocolVersion the client sent in ClientHandshakeC2S matched this server's
// NetworkChannels.PROTOCOL_VERSION, computed by the handler in ServerPacketHandlers, not blocking
// the handshake reply either way. It's placed right after protocolVersion since the two are about
// the same thing (this connection's protocol match); isOperator is unrelated and stays last.
public record ServerHandshakeS2C(int protocolVersion, boolean protocolCompatible, boolean isOperator) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ServerHandshakeS2C> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.HANDSHAKE_S2C);

	public static final StreamCodec<RegistryFriendlyByteBuf, ServerHandshakeS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ServerHandshakeS2C::protocolVersion,
			ByteBufCodecs.BOOL, ServerHandshakeS2C::protocolCompatible,
			ByteBufCodecs.BOOL, ServerHandshakeS2C::isOperator,
			ServerHandshakeS2C::new
	);

	@Override
	public CustomPacketPayload.Type<ServerHandshakeS2C> type() {
		return TYPE;
	}
}
