package com.skyframework.islandcore.command;

import com.mojang.authlib.GameProfile;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.party.lifecycle.PartyDisbandRequests;
import com.skyframework.islandcore.party.model.PartyData;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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
						.then(Commands.literal("ally")
								.then(Commands.literal("add")
										.then(Commands.argument("party", StringArgumentType.word())
												.executes(PartyCommand::executeAllyAdd)))
								.then(Commands.literal("remove")
										.then(Commands.argument("party", StringArgumentType.word())
												.executes(PartyCommand::executeAllyRemove))))
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

		source.sendSuccess(() -> Component.literal("Party \"" + party.getName() + "\" creada. Eres el líder."), false);
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
			targetPlayer.sendSystemMessage(Component.literal(player.getGameProfile().getName()
					+ " te ha invitado a su party \"" + party.getName()
					+ "\". Usa /party accept en los próximos 5 minutos para unirte."));
			source.sendSuccess(() -> Component.literal("Invitación enviada a " + targetProfile.getName() + "."), false);
		} else {
			source.sendSuccess(() -> Component.literal(
					"Invitación registrada para " + targetProfile.getName()
							+ " (no está conectado ahora mismo, pero podrá aceptarla si entra en los próximos 5 minutos)."), false);
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
			source.sendFailure(Component.literal("No tienes ninguna invitación de party pendiente (o ha caducado)."));
			return 0;
		}
		PartyData party = maybeParty.get();

		source.sendSuccess(() -> Component.literal("¡Te has unido a la party \"" + party.getName() + "\"!"), false);

		ServerPlayer leader = source.getServer().getPlayerList().getPlayer(party.getLeaderUuid());
		if (leader != null) {
			leader.sendSystemMessage(Component.literal(
					player.getGameProfile().getName() + " ha aceptado tu invitación y se ha unido a la party."));
		}

		return 1;
	}

	private static int executeLeave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUUID());
		if (maybeParty.isEmpty()) {
			source.sendFailure(Component.literal("No perteneces a ninguna party."));
			return 0;
		}
		PartyData party = maybeParty.get();
		boolean wasLeader = party.getLeaderUuid().equals(player.getUUID());

		boolean disbanded = IslandCoreMod.PARTY_REGISTRY.leaveParty(player.getUUID());

		if (disbanded) {
			source.sendSuccess(() -> Component.literal("Has salido de la party \"" + party.getName() + "\". Al ser el único miembro, se ha disuelto."), false);
		} else if (wasLeader) {
			Optional<PartyData> updated = IslandCoreMod.PARTY_REGISTRY.getParty(party.getPartyId());
			String newLeaderName = updated.map(p -> resolveName(source.getServer(), p.getLeaderUuid())).orElse("otro miembro");
			source.sendSuccess(() -> Component.literal(
					"Has salido de la party \"" + party.getName() + "\". El liderazgo ha pasado a " + newLeaderName + "."), false);
		} else {
			source.sendSuccess(() -> Component.literal("Has salido de la party \"" + party.getName() + "\"."), false);
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
			source.sendFailure(Component.literal("No puedes expulsarte a ti mismo. Usa /party leave o /party disband."));
			return 0;
		}
		if (!party.getMembers().contains(targetProfile.getId())) {
			source.sendFailure(Component.literal(targetProfile.getName() + " no es miembro de tu party."));
			return 0;
		}

		IslandCoreMod.PARTY_REGISTRY.removeMember(party.getPartyId(), targetProfile.getId());

		ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayer(targetProfile.getId());
		if (targetPlayer != null) {
			targetPlayer.sendSystemMessage(Component.literal("Has sido expulsado de la party \"" + party.getName() + "\"."));
		}

		source.sendSuccess(() -> Component.literal(targetProfile.getName() + " ha sido expulsado de la party."), false);
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

		source.sendSuccess(() -> Component.literal("Party renombrada a \"" + newName + "\"."), false);
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
		source.sendSuccess(() -> Component.literal(
				"¿Seguro que quieres disolver la party \"" + party.getName() + "\"? Usa /party disband confirm en los próximos "
						+ PartyDisbandRequests.TIMEOUT.toSeconds() + " segundos para confirmar.").withStyle(ChatFormatting.RED), false);
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
			source.sendFailure(Component.literal("No hay ninguna solicitud de disolución pendiente (o ha caducado). Usa /party disband primero."));
			return 0;
		}

		MinecraftServer server = source.getServer();
		for (UUID memberUuid : party.getMembers()) {
			if (memberUuid.equals(player.getUUID())) {
				continue;
			}
			ServerPlayer member = server.getPlayerList().getPlayer(memberUuid);
			if (member != null) {
				member.sendSystemMessage(Component.literal("La party \"" + party.getName() + "\" ha sido disuelta por su líder."));
			}
		}

		IslandCoreMod.PARTY_REGISTRY.disbandParty(party.getPartyId());
		source.sendSuccess(() -> Component.literal("Party \"" + party.getName() + "\" disuelta."), false);
		return 1;
	}

	private static int executeInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<PartyData> maybeParty = IslandCoreMod.PARTY_REGISTRY.getPartyOf(player.getUUID());
		if (maybeParty.isEmpty()) {
			source.sendFailure(Component.literal("No perteneces a ninguna party."));
			return 0;
		}
		PartyData party = maybeParty.get();
		MinecraftServer server = source.getServer();

		source.sendSuccess(() -> Component.literal("=== Party: " + party.getName() + " ===").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), false);
		source.sendSuccess(() -> Component.literal("Líder: " + resolveName(server, party.getLeaderUuid())).withStyle(ChatFormatting.GOLD), false);

		source.sendSuccess(() -> Component.literal("Miembros (" + party.getMembers().size() + "):").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD), false);
		for (UUID memberUuid : party.getMembers()) {
			String name = resolveName(server, memberUuid);
			boolean isLeader = memberUuid.equals(party.getLeaderUuid());
			source.sendSuccess(() -> Component.literal("- " + name + (isLeader ? " (líder)" : "")), false);
		}

		if (party.getAlliedPartyIds().isEmpty()) {
			source.sendSuccess(() -> Component.literal("Parties aliadas: ninguna").withStyle(ChatFormatting.GRAY), false);
		} else {
			source.sendSuccess(() -> Component.literal("Parties aliadas:").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW), false);
			for (UUID alliedPartyId : party.getAlliedPartyIds()) {
				String alliedName = IslandCoreMod.PARTY_REGISTRY.getParty(alliedPartyId)
						.map(PartyData::getName)
						.orElse("(party eliminada)");
				source.sendSuccess(() -> Component.literal("- " + alliedName), false);
			}
		}

		return 1;
	}

	private static int executeAllyAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String targetName = StringArgumentType.getString(ctx, "party");

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUUID());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		Optional<PartyData> maybeTarget = IslandCoreMod.PARTY_REGISTRY.getPartyByName(targetName);
		if (maybeTarget.isEmpty()) {
			source.sendFailure(Component.literal("No existe ninguna party con el nombre \"" + targetName + "\"."));
			return 0;
		}
		PartyData target = maybeTarget.get();

		if (target.getPartyId().equals(party.getPartyId())) {
			source.sendFailure(Component.literal("Tu party no puede aliarse consigo misma."));
			return 0;
		}

		IslandCoreMod.PARTY_REGISTRY.addAlly(party.getPartyId(), target.getPartyId());
		source.sendSuccess(() -> Component.literal("\"" + target.getName() + "\" ahora es aliada de tu party."), false);
		return 1;
	}

	private static int executeAllyRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String targetName = StringArgumentType.getString(ctx, "party");

		Optional<PartyData> maybeParty = requireLeaderOf(source, player.getUUID());
		if (maybeParty.isEmpty()) {
			return 0;
		}
		PartyData party = maybeParty.get();

		Optional<PartyData> maybeTarget = IslandCoreMod.PARTY_REGISTRY.getPartyByName(targetName);
		if (maybeTarget.isEmpty()) {
			source.sendFailure(Component.literal("No existe ninguna party con el nombre \"" + targetName + "\"."));
			return 0;
		}
		PartyData target = maybeTarget.get();

		IslandCoreMod.PARTY_REGISTRY.removeAlly(party.getPartyId(), target.getPartyId());
		source.sendSuccess(() -> Component.literal("\"" + target.getName() + "\" ya no es aliada de tu party."), false);
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
