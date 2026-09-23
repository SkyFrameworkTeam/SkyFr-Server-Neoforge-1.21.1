package com.skyframework.islandcore.net.admin.defaults;

import com.skyframework.islandcore.net.NetworkChannels;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Sets the server-wide default TriState override for one ISLAND_GLOBAL flag ("/island admin flags
// set-default <flag> allow|deny|default" network equivalent — see AdminDefaultsStatusS2C's class
// javadoc, this is the piece that used to be command-only). Wire field order: flagId, value
// ("allow"/"deny"/"default", same TriState#name().toLowerCase() vocabulary FlagSetC2S already uses
// for an island's own override). Operator-only; ROLE_BASED flags aren't reachable through this
// payload — see AdminFlagSetServerDefaultC2S for those.
public record AdminGlobalFlagSetServerDefaultC2S(String flagId, String value) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AdminGlobalFlagSetServerDefaultC2S> TYPE =
			new CustomPacketPayload.Type<>(NetworkChannels.ADMIN_GLOBAL_FLAG_SET_SERVER_DEFAULT_C2S);

	public static final StreamCodec<RegistryFriendlyByteBuf, AdminGlobalFlagSetServerDefaultC2S> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, AdminGlobalFlagSetServerDefaultC2S::flagId,
			ByteBufCodecs.STRING_UTF8, AdminGlobalFlagSetServerDefaultC2S::value,
			AdminGlobalFlagSetServerDefaultC2S::new
	);

	@Override
	public CustomPacketPayload.Type<AdminGlobalFlagSetServerDefaultC2S> type() {
		return TYPE;
	}
}
