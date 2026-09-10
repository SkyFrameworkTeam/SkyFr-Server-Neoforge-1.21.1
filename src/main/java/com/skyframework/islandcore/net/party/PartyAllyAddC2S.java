package com.skyframework.islandcore.net.party;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// targetPartyName instead of a UUID: parties (unlike players) have no client-side UUID cache to
// pick from outside of PartyStatusS2C#alliedParties (which is the OUTPUT of this action, not an
// input source) — resolved server-side by PartyRegistry#getPartyByName, same as "/party ally add".
public record PartyAllyAddC2S(String targetPartyName) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<PartyAllyAddC2S> TYPE = new CustomPacketPayload.Type<>(NetworkChannels.PARTY_ALLY_ADD_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, PartyAllyAddC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, PartyAllyAddC2S::targetPartyName,
			PartyAllyAddC2S::new
	);

	@Override
	public CustomPacketPayload.Type<PartyAllyAddC2S> type() {
		return TYPE;
	}
}
