package com.skyframework.islandcore.net.island;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// settingId matches IslandSetting#getId() (the same string IslandCommand's "settings" argument
// already accepts, e.g. "firespread"/"pvp"/"mobdamage") — resolved server-side the same way, via
// IslandSetting.fromId(settingId).
public record IslandSettingsUpdateC2S(String settingId, boolean value) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<IslandSettingsUpdateC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ISLAND_SETTINGS_UPDATE_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, IslandSettingsUpdateC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, IslandSettingsUpdateC2S::settingId,
			ByteBufCodecs.BOOL, IslandSettingsUpdateC2S::value,
			IslandSettingsUpdateC2S::new
	);

	@Override
	public CustomPacketPayload.Type<IslandSettingsUpdateC2S> type() {
		return TYPE;
	}
}
