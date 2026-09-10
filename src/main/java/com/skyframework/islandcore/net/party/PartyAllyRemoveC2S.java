package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// targetPartyName instead of a UUID: same reasoning as PartyAllyAddC2S.
public record PartyAllyRemoveC2S(String targetPartyName) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyAllyRemoveC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.PARTY_ALLY_REMOVE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyAllyRemoveC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, PartyAllyRemoveC2S::targetPartyName,
			PartyAllyRemoveC2S::new
	);

	@Override
	public CustomPacketPayload.Type<PartyAllyRemoveC2S> type() {
		return TYPE;
	}
}
