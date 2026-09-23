package com.skyframework.islandcore.net.alliance;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// The receiving player's own four location-sharing toggles (see PlayerLocationSharingConfig): two
// independent pairs, party and allies, each with its own send/receive half.
//
// Wire field order: sendPositionToParty, receivePositionsFromParty, sendPositionToAllies,
// receivePositionsFromAllies.
public record LocationSharingStatusS2C(
		boolean sendPositionToParty,
		boolean receivePositionsFromParty,
		boolean sendPositionToAllies,
		boolean receivePositionsFromAllies
) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LocationSharingStatusS2C> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.LOCATION_SHARING_STATUS_S2C);

	public static final StreamCodec<RegistryFriendlyByteBuf, LocationSharingStatusS2C> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, LocationSharingStatusS2C::sendPositionToParty,
			ByteBufCodecs.BOOL, LocationSharingStatusS2C::receivePositionsFromParty,
			ByteBufCodecs.BOOL, LocationSharingStatusS2C::sendPositionToAllies,
			ByteBufCodecs.BOOL, LocationSharingStatusS2C::receivePositionsFromAllies,
			LocationSharingStatusS2C::new
	);

	@Override
	public CustomPacketPayload.Type<LocationSharingStatusS2C> type() {
		return TYPE;
	}
}
