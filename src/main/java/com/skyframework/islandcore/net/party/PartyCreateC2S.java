package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Wire field order: name.
public record PartyCreateC2S(String name) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyCreateC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.PARTY_CREATE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyCreateC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, PartyCreateC2S::name,
			PartyCreateC2S::new
	);

	@Override
	public CustomPacketPayload.Type<PartyCreateC2S> type() {
		return TYPE;
	}
}
