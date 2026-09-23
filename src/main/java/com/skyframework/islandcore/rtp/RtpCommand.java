package com.skyframework.islandcore.rtp;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.util.ServerLang;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.time.Duration;

// "/rtp", open to every player (no OP required).
//
// The actual cooldown/safe-spot/teleport logic now lives in TeleportManager#requestRtp (moved
// there this sprint so the future network packet handler can reuse it without a second, possibly
// diverging cooldown tracker) — this class only formats the exact same chat messages as before
// from the returned ActionOutcome.
public class RtpCommand {

	private RtpCommand() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
				event.getDispatcher().register(Commands.literal("rtp")
						.executes(RtpCommand::executeRtp))
		);
	}

	private static int executeRtp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		ActionOutcome<Long> outcome = IslandCoreMod.TELEPORT_MANAGER.requestRtp(player);
		if (!outcome.success()) {
			switch (outcome.reason()) {
				case ActionReason.RTP_DISABLED -> source.sendFailure(ServerLang.of(player,
						"El comando /rtp está desactivado en este servidor.", "The /rtp command is disabled on this server."));
				case ActionReason.RTP_DIMENSION_NOT_ALLOWED -> source.sendFailure(ServerLang.of(player,
						"No puedes usar /rtp en esta dimensión.", "You can't use /rtp in this dimension."));
				case ActionReason.COOLDOWN_ACTIVE -> {
					Duration remaining = Duration.ofSeconds(outcome.data());
					source.sendFailure(ServerLang.of(player,
							"Todavía no puedes volver a usar /rtp. Podrás hacerlo en " + formatDuration(remaining, true) + ".",
							"You can't use /rtp again yet. You'll be able to in " + formatDuration(remaining, false) + "."));
				}
				case ActionReason.RTP_NO_SAFE_LOCATION -> source.sendFailure(ServerLang.of(player,
						"No se ha podido encontrar un lugar seguro. Inténtalo de nuevo.", "Couldn't find a safe location. Try again."));
				default -> throw new IllegalStateException("Unhandled ActionReason from requestRtp: " + outcome.reason());
			}
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player,
				"¡Teletransportado a una ubicación aleatoria!", "Teleported to a random location!"), false);

		return 1;
	}

	private static String formatDuration(Duration duration, boolean spanish) {
		long totalSeconds = Math.max(1, duration.getSeconds());
		long days = totalSeconds / 86400;
		long hours = (totalSeconds % 86400) / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (spanish) {
			if (days > 0) {
				return days + (days == 1 ? " día " : " días ") + hours + (hours == 1 ? " hora" : " horas");
			}
			if (hours > 0) {
				return hours + (hours == 1 ? " hora " : " horas ") + minutes + (minutes == 1 ? " minuto" : " minutos");
			}
			if (minutes > 0) {
				return minutes + (minutes == 1 ? " minuto " : " minutos ") + seconds + (seconds == 1 ? " segundo" : " segundos");
			}
			return seconds + (seconds == 1 ? " segundo" : " segundos");
		}

		if (days > 0) {
			return days + (days == 1 ? " day " : " days ") + hours + (hours == 1 ? " hour" : " hours");
		}
		if (hours > 0) {
			return hours + (hours == 1 ? " hour " : " hours ") + minutes + (minutes == 1 ? " minute" : " minutes");
		}
		if (minutes > 0) {
			return minutes + (minutes == 1 ? " minute " : " minutes ") + seconds + (seconds == 1 ? " second" : " seconds");
		}
		return seconds + (seconds == 1 ? " second" : " seconds");
	}
}
