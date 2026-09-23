package com.skyframework.islandcore.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

// Server-side literal-message localization (Sprint "teletransportes dinámicos" point A): picks
// between a Spanish and an English literal based on the RECEIVING player's own client language
// (ServerPlayerEntity#getClientOptions().language(), sent by vanilla's own protocol on join/options
// change — works for ANY client, modded or vanilla, no IslandCoreClient dependency required).
//
// Text.translatable(...) is deliberately NOT used for this: a translation key only resolves against
// lang files the CLIENT MOD ships, which a vanilla client doesn't have — it would just see the raw
// key. Text.literal(...) with a value picked server-side is the only approach that works for every
// client uniformly.
//
// Always pass the player who will actually SEE the message (the recipient), not necessarily the
// player who triggered the action — e.g. an island owner being notified about something a member
// did. Console/log-only messages (LOGGER.info/error, no ServerPlayerEntity involved) are out of
// scope for this helper and stay Spanish-only, since only the server admin reads them.
public final class ServerLang {

	private ServerLang() {
	}

	public static Component of(ServerPlayer player, String es, String en) {
		String language = player.clientInformation().language();
		boolean spanish = language != null && language.toLowerCase(Locale.ROOT).startsWith("es");
		return Component.literal(spanish ? es : en);
	}
}
