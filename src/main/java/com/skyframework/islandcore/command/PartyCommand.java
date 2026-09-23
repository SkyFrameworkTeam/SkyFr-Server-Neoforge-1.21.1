package com.skyframework.islandcore.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.party.lifecycle.PartyDisbandRequests;
import com.skyframework.islandcore.party.model.PartyData;
import com.skyframework.islandcore.util.ServerLang;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;
import java.util.UUID;

// The /party command tree: player-facing, no OP requirement. Reuses the same registry+storage
// shape as /dimension (see PartyRegistryImpl) rather than the IslandRegistry/MembershipService
// path used by /island, since parties are deliberately independent of island/.
public class PartyCommand {

	private PartyCommand() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
				event.getDispatcher().register(Commands.literal("party")
						.then(Commands.literal("create")
								.then(Commands.argument("name", StringArgumentType.word())
										.executes(PartyCommand::executeCreate)))
						.then(Commands.literal("invite")
								.then(Commands.argument("player", GameProfileArgument.gameProfile())
										.executes(PartyCommand::executeInvite)))
						.then(Commands.literal("accept")
								.executes(PartyCommand::executeAccept))
						.then(Commands.literal("leave")
								.executes(PartyCommand::executeLeave))
						.then(Commands.literal("kick")
								.then(Commands.argument("player", GameProfileArgument.gameProfile())
										.executes(PartyCommand::executeKick)))
						.then(Commands.literal("rename")
								.then(Commands.argument("name", StringArgumentType.word())
										.executes(PartyCommand::executeRename)))
						.then(Commands.literal("disband")
								.executes(PartyCommand::executeDisband)
								.then(Commands.literal("confirm")
										.executes(PartyCommand::executeDisbandConfirm)))
						.then(Commands.literal("info")
								.executes(PartyCommand::executeInfo))
				)
		);
	}

	private static int executeCreate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String name = StringArgumentType.getString(ctx, "name");

		PartyData party;
		try {
			party = IslandCoreMod.PARTY_REGISTRY.createParty(name, player.getUUID());
		} catch (IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player, "Party \"" + party.getName() + "\" creada. Eres el líder.", "Party \"" + party.getName() + "\" created. You are the leader."), false);
		return 1;
	}

	private static int executeInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		GameProfile targetProfile = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUUID());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		try {
			IslandCoreMod.PARTY_INVITE_MANAGER.requestInvite(party.getPartyId(), player.getUUID(), targetProfile.getId());
		} catch (IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayer(targetProfile.getId());
		if (targetPlayer != null) {
			targetPlayer.sendSystemMessage(ServerLang.of(targetPlayer, player.getGameProfile().getName()
					+ " te ha invitado a su party \"" + party.getName()
					+ "\". Usa /party accept en los próximos 5 minutos para unirte.",
					player.getGameProfile().getName()
					+ " has invited you to their party \"" + party.getName()
					+ "\". Use /party accept within the next 5 minutes to join."));
			source.sendSuccess(() -> ServerLang.of(player, "Invitación enviada a " + targetProfile.getName() + ".", "Invitation sent to " + targetProfile.getName() + "."), false);
		} else {
			source.sendSuccess(() -> ServerLang.of(player,
					"Invitación registrada para " + targetProfile.getName()
							+ " (no está conectado ahora mismo, pero podrá aceptarla si entra en los próximos 5 minutos).",
					"Invitation registered for " + targetProfile.getName()
							+ " (they are not connected right now, but will be able to accept it if they join within the next 5 minutes)."), false);
		}

		return 1;
	}

	private static int executeAccept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<PartyData> maybeParty;
		try {
			maybeParty = IslandCoreMod.PARTY_INVITE_MANAGER.acceptInvite(player.getUUID());
		} catch (IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		if (maybeParty.isEmpty()) {
			source.sendFailure(ServerLang.of(player, "No tienes ninguna invitación de party pendiente (o ha caducado).", "You do not have any pending party invitation (or it has expired)."));
			return 0;
		}
		PartyData party = maybeParty.get();

		source.sendSuccess(() -> ServerLang.of(player, "¡Te has unido a la party \"" + party.getName() + "\"!", "You have joined the party \"" + party.getName() + "\"!"), false);

		ServerPlayer leader = source.getServer().getPlayerList().getPlayer(party.getLeaderUuid());
		if (leader != null) {
			leader.sendSystemMessage(ServerLang.of(leader,
					player.getGameProfile().getName() + " ha aceptado tu invitación y se ha unido a la party.",
					player.getGameProfile().getName() + " has accepted your invitation and joined the party."));
		}

		return 1;
	}

	private static int executeLeave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUUID());
		if (maybeParty.isEmpty()) {
			source.sendFailure(ServerLang.of(player, "No perteneces a ninguna party.", "You do not belong to any party."));
			return 0;
		}
		PartyData party = maybeParty.get();
		boolean wasLeader = party.getLeaderUuid().equals(player.getUUID());

		boolean disbanded = IslandCoreMod.PARTY_REGISTRY.leaveParty(player.getUUID());

		if (disbanded) {
			source.sendSuccess(() -> ServerLang.of(player, "Has salido de la party \"" + party.getName() + "\". Al ser el único miembro, se ha disuelto.", "You have left the party \"" + party.getName() + "\". Since you were the only member, it has been disbanded."), false);
		} else if (wasLeader) {
			Optional<PartyData> updated = IslandCoreMod.PARTY_REGISTRY.getParty(party.getPartyId());
			String newLeaderName = updated.map(p -> resolveName(source.getServer(), p.getLeaderUuid())).orElse("otro miembro");
			source.sendSuccess(() -> ServerLang.of(player,
					"Has salido de la party \"" + party.getName() + "\". El liderazgo ha pasado a " + newLeaderName + ".",
					"You have left the party \"" + party.getName() + "\". Leadership has passed to " + newLeaderName + "."), false);
		} else {
			source.sendSuccess(() -> ServerLang.of(player, "Has salido de la party \"" + party.getName() + "\".", "You have left the party \"" + party.getName() + "\"."), false);
		}

		return 1;
	}

	private static int executeKick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		GameProfile targetProfile = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUUID());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		if (targetProfile.getId().equals(player.getUUID())) {
			source.sendFailure(ServerLang.of(player, "No puedes expulsarte a ti mismo. Usa /party leave o /party disband.", "You cannot kick yourself. Use /party leave or /party disband."));
			return 0;
		}
		if (!party.getMembers().contains(targetProfile.getId())) {
			source.sendFailure(ServerLang.of(player, targetProfile.getName() + " no es miembro de tu party.", targetProfile.getName() + " is not a member of your party."));
			return 0;
		}

		IslandCoreMod.PARTY_REGISTRY.removeMember(party.getPartyId(), targetProfile.getId());

		ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayer(targetProfile.getId());
		if (targetPlayer != null) {
			targetPlayer.sendSystemMessage(ServerLang.of(targetPlayer, "Has sido expulsado de la party \"" + party.getName() + "\".", "You have been kicked from the party \"" + party.getName() + "\"."));
		}

		source.sendSuccess(() -> ServerLang.of(player, targetProfile.getName() + " ha sido expulsado de la party.", targetProfile.getName() + " has been kicked from the party."), false);
		return 1;
	}

	private static int executeRename(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String newName = StringArgumentType.getString(ctx, "name");

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUUID());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		try {
			IslandCoreMod.PARTY_REGISTRY.renameParty(party.getPartyId(), newName);
		} catch (IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player, "Party renombrada a \"" + newName + "\".", "Party renamed to \"" + newName + "\"."), false);
		return 1;
	}

	private static int executeDisband(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUUID());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		PartyDisbandRequests.request(party.getPartyId());
		source.sendSuccess(() -> ((net.minecraft.network.chat.MutableComponent) ServerLang.of(player,
				"¿Seguro que quieres disolver la party \"" + party.getName() + "\"? Usa /party disband confirm en los próximos "
						+ PartyDisbandRequests.TIMEOUT.toSeconds() + " segundos para confirmar.",
				"Are you sure you want to disband the party \"" + party.getName() + "\"? Use /party disband confirm within the next "
						+ PartyDisbandRequests.TIMEOUT.toSeconds() + " seconds to confirm.")).withStyle(ChatFormatting.RED), false);
		return 1;
	}

	private static int executeDisbandConfirm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUUID());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		if (!PartyDisbandRequests.confirm(party.getPartyId())) {
			source.sendFailure(ServerLang.of(player, "No hay ninguna solicitud de disolución pendiente (o ha caducado). Usa /party disband primero.", "There is no pending disband request (or it has expired). Use /party disband first."));
			return 0;
		}

		MinecraftServer server = source.getServer();
		for (UUID memberUuid : party.getMembers()) {
			if (memberUuid.equals(player.getUUID())) {
				continue;
			}
			ServerPlayer member = server.getPlayerList().getPlayer(memberUuid);
			if (member != null) {
				member.sendSystemMessage(ServerLang.of(member, "La party \"" + party.getName() + "\" ha sido disuelta por su líder.", "The party \"" + party.getName() + "\" has been disbanded by its leader."));
			}
		}

		IslandCoreMod.PARTY_REGISTRY.disbandParty(party.getPartyId());
		source.sendSuccess(() -> ServerLang.of(player, "Party \"" + party.getName() + "\" disuelta.", "Party \"" + party.getName() + "\" disbanded."), false);
		return 1;
	}

	private static int executeInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUUID());
		if (maybeParty.isEmpty()) {
			source.sendFailure(ServerLang.of(player, "No perteneces a ninguna party.", "You do not belong to any party."));
			return 0;
		}
		PartyData party = maybeParty.get();
		MinecraftServer server = source.getServer();

		source.sendSuccess(() -> ((net.minecraft.network.chat.MutableComponent) ServerLang.of(player, "=== Party: " + party.getName() + " ===", "=== Party: " + party.getName() + " ===")).withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), false);
		source.sendSuccess(() -> ((net.minecraft.network.chat.MutableComponent) ServerLang.of(player, "Líder: " + resolveName(server, party.getLeaderUuid()), "Leader: " + resolveName(server, party.getLeaderUuid()))).withStyle(ChatFormatting.GOLD), false);

		source.sendSuccess(() -> ((net.minecraft.network.chat.MutableComponent) ServerLang.of(player, "Miembros (" + party.getMembers().size() + "):", "Members (" + party.getMembers().size() + "):")).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), false);
		for (UUID memberUuid : party.getMembers()) {
			String name = resolveName(server, memberUuid);
			boolean isLeader = memberUuid.equals(party.getLeaderUuid());
			source.sendSuccess(() -> ServerLang.of(player, "- " + name + (isLeader ? " (líder)" : ""), "- " + name + (isLeader ? " (leader)" : "")), false);
		}

		return 1;
	}

	// Shared guard for every leader-only subcommand: reports "no tienes party"/"no eres el líder"
	// and returns empty on failure, or the executor's party on success.
	private static Optional<PartyData> requireLeaderOf(CommandSourceStack source, UUID playerUuid) {
		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(playerUuid);
		if (maybeParty.isEmpty()) {
			source.sendFailure(Component.literal("No perteneces a ninguna party."));
			return Optional.empty();
		}

		PartyData party = maybeParty.get();
		if (!party.getLeaderUuid().equals(playerUuid)) {
			source.sendFailure(Component.literal("Solo el líder de la party puede hacer esto."));
			return Optional.empty();
		}

		return Optional.of(party);
	}

	private static String resolveName(MinecraftServer server, UUID playerUuid) {
		return server.getProfileCache().get(playerUuid).map(GameProfile::getName).orElse(playerUuid.toString());
	}
}
