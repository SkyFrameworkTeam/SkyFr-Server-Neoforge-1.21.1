package com.skyframework.islandcore.net.handshake;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server-side copy of IslandCoreClient's network.handshake.ClientHandshakeC2S. Fabric's
// networking has no shared-payload mechanism across separate mod jars, so both sides
// independently define the same channel id and wire format; they must be kept in sync by hand.
public record ClientHandshakeC2S(int protocolVersion) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ClientHandshakeC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.HANDSHAKE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientHandshakeC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ClientHandshakeC2S::protocolVersion,
			ClientHandshakeC2S::new
	);

	@Override
	public CustomPacketPayload.Type<ClientHandshakeC2S> type() {
		return TYPE;
	}
}
