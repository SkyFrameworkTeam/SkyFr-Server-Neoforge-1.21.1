package com.skyframework.islandcore.spawn;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

// "/spawn", open to every player (no OP required).
public class SpawnCommand {

	private SpawnCommand() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
				event.getDispatcher().register(Commands.literal("spawn")
						.executes(SpawnCommand::executeSpawn))
		);
	}

	private static int executeSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		if (!IslandCoreMod.SPAWN_CONFIG.isEnabled()) {
			source.sendFailure(Component.literal("El comando /spawn está desactivado en este servidor."));
			return 0;
		}

		// requestSpawn() sends its own feedback/error messages directly to the player (it may
		// need to message them again later, when the warmup finishes or is cancelled).
		IslandCoreMod.TELEPORT_MANAGER.requestSpawn(player);

		return 1;
	}
}
