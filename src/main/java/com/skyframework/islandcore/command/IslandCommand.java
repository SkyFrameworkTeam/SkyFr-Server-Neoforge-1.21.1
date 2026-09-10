package com.skyframework.islandcore.command;

import com.mojang.authlib.GameProfile;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.api.network.ActionOutcome;
import com.skyframework.islandcore.api.network.ActionReason;
import com.skyframework.islandcore.api.permission.IslandPermissions;
import com.skyframework.islandcore.island.lifecycle.IslandActionService;
import com.skyframework.islandcore.island.lifecycle.MembershipService;
import com.skyframework.islandcore.island.model.IslandRole;
import com.skyframework.islandcore.island.model.IslandSetting;
import com.skyframework.islandcore.protection.exception.ExceptionGroup;
import com.skyframework.islandcore.protection.exception.ExceptionGroupCategory;
import com.skyframework.islandcore.protection.exception.ExceptionResolver;
import com.skyframework.islandcore.protection.flag.Flag;
import com.skyframework.islandcore.protection.flag.FlagCategory;
import com.skyframework.islandcore.protection.flag.FlagPreset;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;
import com.skyframework.islandcore.protection.flag.TriState;
import com.skyframework.islandcore.teleport.SafeLandingChecker;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// The one and only /island command tree. The old /ic and /islandcore debug tree
// (command.debug.IslandDebugCommand) has been fully retired as of Sprint 12.
public class IslandCommand {

	private static final ResourceKey<Level> ISLANDS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("islandcore", "islands"));

	private static final List<String> SETTING_NAMES = List.of("firespread", "pvp", "mobdamage");
	private static final List<String> TRISTATE_NAMES = List.of("allow", "deny", "default");
	private static final List<String> FLAG_PRESET_NAMES = List.of("nadie", "miembros", "aliados", "todos");
	// "/island admin flags set-default" accepts either vocabulary depending on the flag's category
	// (ISLAND_GLOBAL -> allow/deny, ROLE_BASED -> preset) — see executeAdminFlagsSetDefault and
	// suggestFlagSetDefaultValues, which reads the already-typed "flag" argument to offer only the
	// vocabulary that flag actually accepts, instead of both unconditionally. This full union is
	// kept only as the fallback for an unrecognized/not-yet-typed flag id.
	private static final List<String> ALLOW_DENY_NAMES = List.of("allow", "deny");
	private static final List<String> FLAG_SET_DEFAULT_VALUE_NAMES =
			List.of("allow", "deny", "nadie", "miembros", "aliados", "todos");

	private IslandCommand() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
				event.getDispatcher().register(Commands.literal("island")
						.then(Commands.literal("create")
								.executes(IslandCommand::executeCreate))
						.then(Commands.literal("info")
								.executes(IslandCommand::executeInfo))
						.then(Commands.literal("list")
								.executes(IslandCommand::executeList))
						.then(Commands.literal("upgrade")
								.executes(IslandCommand::executeUpgrade))
						.then(Commands.literal("home")
								.executes(IslandCommand::executeHome))
						.then(Commands.literal("sethome")
								.executes(IslandCommand::executeSetHome))
						.then(Commands.literal("settings")
								.then(Commands.argument("setting", StringArgumentType.word())
										.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SETTING_NAMES, builder))
										.then(Commands.argument("value", BoolArgumentType.bool())
												.executes(IslandCommand::executeSettings))))
						.then(Commands.literal("delete")
								.executes(IslandCommand::executeDelete)
								.then(Commands.literal("confirm")
										.executes(IslandCommand::executeDeleteConfirm)))
						.then(Commands.literal("limits")
								.executes(IslandCommand::executeLimits))
						.then(Commands.literal("invite")
								.then(Commands.argument("player", GameProfileArgument.gameProfile())
										.executes(IslandCommand::executeInvite)))
						.then(Commands.literal("accept")
								.executes(IslandCommand::executeAccept))
						.then(Commands.literal("trust")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(IslandCommand::executeTrust)))
						.then(Commands.literal("untrust")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(IslandCommand::executeUntrust)))
						.then(Commands.literal("ally")
								.then(Commands.literal("add")
										.then(Commands.argument("player", EntityArgument.player())
												.executes(IslandCommand::executeAllyAdd)))
								.then(Commands.literal("remove")
										.then(Commands.argument("player", EntityArgument.player())
												.executes(IslandCommand::executeAllyRemove))))
						.then(Commands.literal("kick")
								.then(Commands.argument("player", GameProfileArgument.gameProfile())
										.executes(IslandCommand::executeKick)))
						.then(Commands.literal("biome")
								.then(Commands.argument("biome", ResourceLocationArgument.id())
										.suggests(IslandCommand::suggestBiomes)
										.executes(IslandCommand::executeBiome)))
						.then(Commands.literal("flags")
								.executes(IslandCommand::executeFlagsList)
								.then(Commands.literal("set")
										.then(Commands.argument("flag", StringArgumentType.word())
												.suggests(IslandCommand::suggestFlagIds)
												.then(Commands.argument("value", StringArgumentType.word())
														.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TRISTATE_NAMES, builder))
														.executes(IslandCommand::executeFlagsSet))))
								.then(Commands.literal("preset")
										.then(Commands.argument("flag", StringArgumentType.word())
												.suggests(IslandCommand::suggestRoleBasedFlagIds)
												.then(Commands.argument("preset", StringArgumentType.word())
														.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FLAG_PRESET_NAMES, builder))
														.executes(IslandCommand::executeFlagsPreset)))))
						.then(Commands.literal("exceptions")
								.then(Commands.literal("list")
										.executes(IslandCommand::executeExceptionsList))
								.then(Commands.literal("preset")
										.then(Commands.argument("group", StringArgumentType.word())
												.suggests(IslandCommand::suggestExceptionGroupIds)
												.then(Commands.argument("preset", StringArgumentType.word())
														.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FLAG_PRESET_NAMES, builder))
														.executes(IslandCommand::executeExceptionsPreset)))))
						.then(Commands.literal("admin")
								.requires(source -> source.hasPermission(2))
								.then(Commands.literal("spawn")
										.then(Commands.literal("create")
												.then(Commands.argument("size", IntegerArgumentType.integer(1))
														.executes(IslandCommand::executeAdminSpawnCreate)))
										.then(Commands.literal("resize")
												.then(Commands.argument("size", IntegerArgumentType.integer(1))
														.executes(IslandCommand::executeAdminSpawnResize)))
										.then(Commands.literal("sethome")
												.executes(IslandCommand::executeAdminSpawnSetHome))
										.then(Commands.literal("settings")
												.then(Commands.literal("buildprotection")
														.then(Commands.argument("value", BoolArgumentType.bool())
																.executes(IslandCommand::executeAdminSpawnBuildProtection))))
										.then(Commands.literal("trust")
												.then(Commands.argument("player", EntityArgument.player())
														.executes(IslandCommand::executeAdminSpawnTrust)))
										.then(Commands.literal("untrust")
												.then(Commands.argument("player", EntityArgument.player())
														.executes(IslandCommand::executeAdminSpawnUntrust)))
										.then(Commands.literal("exceptions")
												.then(Commands.literal("preset")
														.then(Commands.argument("group", StringArgumentType.word())
																.suggests(IslandCommand::suggestExceptionGroupIds)
																.then(Commands.argument("preset", StringArgumentType.word())
																		.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FLAG_PRESET_NAMES, builder))
																		.executes(IslandCommand::executeAdminSpawnExceptionsPreset))))))
								.then(Commands.literal("flags")
										.then(Commands.literal("set-default")
												.then(Commands.argument("flag", StringArgumentType.word())
														.suggests(IslandCommand::suggestFlagIds)
														.then(Commands.argument("value", StringArgumentType.word())
																.suggests(IslandCommand::suggestFlagSetDefaultValues)
																.executes(IslandCommand::executeAdminFlagsSetDefault))))
										.then(Commands.literal("require")
												.then(Commands.argument("flag", StringArgumentType.word())
														.suggests(IslandCommand::suggestFlagIds)
														.then(Commands.argument("permission", StringArgumentType.string())
																.executes(IslandCommand::executeAdminFlagsRequire)))))
								.then(Commands.literal("exceptions")
										.then(Commands.literal("set-default")
												.then(Commands.argument("group", StringArgumentType.word())
														.suggests(IslandCommand::suggestExceptionGroupIds)
														.then(Commands.argument("preset", StringArgumentType.word())
																.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FLAG_PRESET_NAMES, builder))
																.executes(IslandCommand::executeAdminExceptionsSetDefault)))))
								.then(Commands.literal("list")
										.executes(IslandCommand::executeAdminListAll)
										.then(Commands.argument("player", EntityArgument.player())
												.executes(IslandCommand::executeAdminListPlayer)))
								.then(Commands.literal("delete")
										.then(Commands.argument("player", EntityArgument.player())
												.executes(IslandCommand::executeAdminDelete)
												.then(Commands.literal("confirm")
														.executes(IslandCommand::executeAdminDeleteConfirm)))))
				)
		);
	}

	private static int executeCreate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		ActionOutcome<Island> outcome = IslandActionService.create(player, source.getServer());
		if (!outcome.success()) {
			if (ActionReason.NO_PERMISSION.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes permitido crear una isla en este servidor."));
			} else {
				source.sendFailure(Component.literal("Ya tienes una isla. Usa /island info para ver sus datos."));
			}
			return 0;
		}

		BlockPos center = outcome.data().getCenter();
		source.sendSuccess(() -> Component.literal("¡Isla creada! Centro: ("
				+ center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"), false);

		return 1;
	}

	private static int executeInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUUID());
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("You don't own an island."));
			return 0;
		}

		IslandMessages.sendIslandSummaryPlayer(source, maybeIsland.get());
		return 1;
	}

	private static int executeList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUUID());
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("No tienes ninguna isla todavía. Usa /island create para crear una."));
			return 0;
		}

		IslandMessages.sendIslandSummaryPlayer(source, maybeIsland.get());
		return 1;
	}

	private static int executeUpgrade(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		ActionOutcome<IslandActionService.UpgradeResult> outcome = IslandActionService.upgrade(player.getUUID());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else {
				// maxSize exceeds the island's reserved plot: a permissions/plot misconfiguration,
				// not something the player can fix themselves.
				source.sendFailure(Component.literal(
						"El tamaño permitido por tus permisos excede la parcela reservada de tu isla. Contacta con un administrador del servidor."));
			}
			return 0;
		}

		IslandActionService.UpgradeResult result = outcome.data();
		if (result.oldSize() == result.newSize()) {
			int currentSize = result.oldSize();
			source.sendSuccess(() -> Component.literal(
					"Ya tienes el tamaño máximo permitido por tus permisos actuales (tamaño actual: " + currentSize + ")."), false);
			return 1;
		}

		int newSize = result.newSize();
		source.sendSuccess(() -> Component.literal("¡Isla ampliada! Nuevo tamaño: " + newSize + "."), false);

		return 1;
	}

	private static int executeHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		// requestHome() sends its own feedback/error messages directly to the player
		// (it may need to message them again later, when the warmup finishes or is cancelled).
		IslandCoreMod.TELEPORT_MANAGER.requestHome(player);

		return 1;
	}

	private static int executeSetHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUUID());
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		Island island = maybeIsland.get();
		boolean inIslandsDimension = player.level().dimension().equals(ISLANDS_DIMENSION);
		boolean withinBuiltIsland = island.getBounds().contains(player.blockPosition());

		if (!inIslandsDimension || !withinBuiltIsland) {
			source.sendFailure(Component.literal("El home debe fijarse dentro de la parte ya construida de tu isla."));
			return 0;
		}

		if (!SafeLandingChecker.isSafe(player.serverLevel(), player.blockPosition())) {
			source.sendFailure(Component.literal(
					"No puedes fijar el home aquí, no hay suelo seguro debajo. Colócate sobre un bloque sólido."));
			return 0;
		}

		IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), player.blockPosition());

		source.sendSuccess(() -> Component.literal("Home actualizado a tu posición actual."), false);

		return 1;
	}

	// Delegates entirely to IslandActionService#updateLegacySetting — the SAME method
	// IslandSettingsUpdateC2S's network handler calls — so this command never decides on its own
	// whether a setting id maps to the new Flag system or the old IslandSetting one; that mapping
	// lives in exactly one place, and both paths stay convergent by construction.
	private static int executeSettings(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String settingName = StringArgumentType.getString(ctx, "setting");
		boolean value = BoolArgumentType.getBool(ctx, "value");

		ActionOutcome<Void> outcome = IslandActionService.updateLegacySetting(player.getUUID(), settingName, value);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.UNKNOWN_SETTING.equals(outcome.reason())) {
				source.sendFailure(Component.literal("Ajuste desconocido. Ajustes disponibles: firespread, pvp, mobdamage."));
			} else {
				source.sendFailure(Component.literal("Solo el propietario de la isla puede cambiar sus ajustes."));
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal(settingName.toLowerCase() + " = " + value), false);

		return 1;
	}

	private static int executeFlagsList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUUID());
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		Island island = maybeIsland.get();
		source.sendSuccess(() -> Component.literal("=== Flags de tu isla ===").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), false);

		IslandMessages.sectionTitle(source, "Permisos por Rol", ChatFormatting.GOLD);
		IslandRole[] roles = IslandRole.values();
		for (Flag flag : FlagRegistry.all()) {
			if (flag.getCategory() != FlagCategory.ROLE_BASED) {
				continue;
			}

			String header = flagDisplayName(flag) + ":";
			source.sendSuccess(() -> Component.literal(header).withStyle(ChatFormatting.WHITE), false);

			// 3 roles per line (OWNER/MEMBER/CO_OWNER, then ALLY/VISITOR/DENIED) rather than one
			// long line: easier to scan, and each ALLOW/DENY value is colored green/red.
			for (int i = 0; i < roles.length; i += 3) {
				int lineEnd = Math.min(i + 3, roles.length);
				MutableComponent line = Component.literal("  ");
				for (int j = i; j < lineEnd; j++) {
					IslandRole role = roles[j];
					TriState value = FlagResolver.resolveForRole(island, role, flag);
					line.append(roleValueText(role, value));
					if (j < lineEnd - 1) {
						line.append(Component.literal("   "));
					}
				}
				source.sendSuccess(() -> line, false);
			}
		}

		IslandMessages.sectionTitle(source, "Ajustes Generales", ChatFormatting.LIGHT_PURPLE);
		for (Flag flag : FlagRegistry.all()) {
			if (flag.getCategory() != FlagCategory.ISLAND_GLOBAL) {
				continue;
			}
			String valueText = FlagResolver.resolveGlobal(island, flag) ? "allow" : "deny";
			String label = flagDisplayName(flag) + ": ";
			source.sendSuccess(() -> IslandMessages.labeled(label, valueText), false);
		}

		return 1;
	}

	private static Component roleValueText(IslandRole role, TriState value) {
		ChatFormatting valueColor = value == TriState.ALLOW ? ChatFormatting.GREEN : ChatFormatting.RED;
		return Component.literal(role.name() + ": ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(value.name()).withStyle(valueColor));
	}

	// Presentation only (Sprint "mejora visual de /island flags") — the flag ids themselves
	// (used by /island flags set and stored on disk) are unchanged.
	private static String flagDisplayName(Flag flag) {
		return switch (flag.getId()) {
			case "construccion" -> "Construcción";
			case "interact" -> "Interactuar";
			case "entities" -> "Entidades";
			case "fire_spread" -> "Propagación de fuego";
			case "pvp_damage" -> "Daño PvP";
			case "mob_damage" -> "Daño de mobs";
			case "explosion_damage" -> "Daño de explosiones";
			case "crop_trample" -> "Pisoteo de cultivos";
			case "natural_mob_spawning" -> "Aparición natural de mobs";
			case "raids" -> "Incursiones (raids)";
			default -> flag.getId();
		};
	}

	private static int executeFlagsSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String flagId = StringArgumentType.getString(ctx, "flag");
		String valueArg = StringArgumentType.getString(ctx, "value");

		Optional<Flag> maybeFlag = FlagRegistry.get(flagId);
		if (maybeFlag.isEmpty()) {
			source.sendFailure(Component.literal("Flag desconocido: " + flagId));
			return 0;
		}

		TriState value;
		try {
			value = TriState.valueOf(valueArg.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal("Valor inválido: " + valueArg + ". Usa allow, deny o default."));
			return 0;
		}

		ActionOutcome<Void> outcome = IslandActionService.updateFlag(player.getUUID(), maybeFlag.get(), value);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.MISSING_FLAG_PERMISSION.equals(outcome.reason())) {
				source.sendFailure(Component.literal(missingFlagPermissionMessage(flagId)));
			} else {
				source.sendFailure(Component.literal("Solo el propietario de la isla puede cambiar sus flags."));
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal(flagId + " = " + valueArg.toLowerCase(Locale.ROOT)), false);

		return 1;
	}

	private static int executeFlagsPreset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String flagId = StringArgumentType.getString(ctx, "flag");
		String preset = StringArgumentType.getString(ctx, "preset");

		ActionOutcome<Void> outcome = IslandActionService.applyFlagPreset(player.getUUID(), flagId, preset);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.INVALID_FLAG_PRESET.equals(outcome.reason())) {
				source.sendFailure(Component.literal(
						"Flag o preset inválido. Usa un flag por rol (no fire_spread/pvp_damage/mob_damage) y uno de: "
								+ String.join(", ", FLAG_PRESET_NAMES) + "."));
			} else if (ActionReason.MISSING_FLAG_PERMISSION.equals(outcome.reason())) {
				source.sendFailure(Component.literal(missingFlagPermissionMessage(flagId)));
			} else {
				source.sendFailure(Component.literal("Solo el propietario de la isla puede cambiar sus flags."));
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal(flagId + " = preset " + preset), false);

		return 1;
	}

	// Shared by executeFlagsSet/executeFlagsPreset: ActionReason.MISSING_FLAG_PERMISSION alone
	// doesn't carry which node was missing (the wire-facing ActionReason keys are static strings,
	// no parameters), so the text-command path re-looks it up here to give a concrete message —
	// the network path's client just sees the generic reason key, translated client-side.
	private static String missingFlagPermissionMessage(String flagId) {
		String node = IslandCoreMod.FLAG_PERMISSION_REQUIREMENTS.getRequiredPermission(flagId).orElse("?");
		return "No tienes el permiso necesario para cambiar " + flagId + " (\"" + node + "\").";
	}

	// Dispatches by the flag's own category: ISLAND_GLOBAL flags (fire_spread/pvp_damage/mob_damage)
	// keep the plain allow/deny value they always had (a preset is meaningless for a flag with no
	// per-role distinction); ROLE_BASED flags now require one of the 4 preset names instead — a flat
	// single value can't express "different per role" the way a preset can.
	private static int executeAdminFlagsSetDefault(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		String flagId = StringArgumentType.getString(ctx, "flag");
		String valueArg = StringArgumentType.getString(ctx, "value");

		Optional<Flag> maybeFlag = FlagRegistry.get(flagId);
		if (maybeFlag.isEmpty()) {
			source.sendFailure(Component.literal("Flag desconocido: " + flagId));
			return 0;
		}
		Flag flag = maybeFlag.get();

		if (flag.getCategory() == FlagCategory.ISLAND_GLOBAL) {
			TriState value;
			try {
				value = TriState.valueOf(valueArg.toUpperCase(Locale.ROOT));
				if (value == TriState.DEFAULT) {
					throw new IllegalArgumentException("default no es válido aquí");
				}
			} catch (IllegalArgumentException e) {
				source.sendFailure(Component.literal("Valor inválido: " + valueArg + ". Usa allow o deny (este flag no admite preset)."));
				return 0;
			}

			IslandCoreMod.SERVER_FLAG_DEFAULTS.setGlobalDefault(flagId, value);
			source.sendSuccess(() -> Component.literal(
					"Valor por defecto del servidor para " + flagId + " = " + valueArg.toLowerCase(Locale.ROOT)), false);
			return 1;
		}

		Optional<FlagPreset> preset = FlagPreset.fromId(valueArg.toLowerCase(Locale.ROOT));
		if (preset.isEmpty()) {
			source.sendFailure(Component.literal("Valor inválido: " + valueArg + ". Usa uno de: "
					+ String.join(", ", FLAG_PRESET_NAMES) + " (este flag es por rol, no admite allow/deny)."));
			return 0;
		}

		IslandCoreMod.SERVER_FLAG_DEFAULTS.setRoleBasedDefault(flagId, preset.get());
		source.sendSuccess(() -> Component.literal(
				"Valor por defecto del servidor para " + flagId + " = preset " + preset.get().getId()), false);
		return 1;
	}

	private static int executeAdminFlagsRequire(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		String flagId = StringArgumentType.getString(ctx, "flag");
		String permissionArg = StringArgumentType.getString(ctx, "permission");

		if (FlagRegistry.get(flagId).isEmpty()) {
			source.sendFailure(Component.literal("Flag desconocido: " + flagId));
			return 0;
		}

		String node = "ninguno".equalsIgnoreCase(permissionArg) ? null : permissionArg;
		IslandCoreMod.FLAG_PERMISSION_REQUIREMENTS.setRequiredPermission(flagId, node);

		source.sendSuccess(() -> Component.literal(node == null
				? "El flag " + flagId + " ya no requiere ningún permiso especial para cambiarlo."
				: "El flag " + flagId + " ahora requiere el permiso \"" + node + "\" para cambiarlo."), false);

		return 1;
	}

	private static int executeExceptionsList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(player.getUUID());
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			return 0;
		}

		Island island = maybeIsland.get();
		List<ExceptionGroup> groups = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getAllGroups();
		if (groups.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No hay ningún grupo de excepción configurado."), false);
			return 0;
		}

		source.sendSuccess(() -> Component.literal("=== Grupos de Excepción de tu Isla ===").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), false);

		sendExceptionGroupSection(source, island, groups, ExceptionGroupCategory.BLOCK, "Bloques");
		sendExceptionGroupSection(source, island, groups, ExceptionGroupCategory.ENTITY, "Entidades");

		return groups.size();
	}

	// Same titled-section look as executeFlagsList (see IslandMessages#sectionTitle), split by
	// ExceptionGroupCategory (BLOCK/ENTITY) instead of by role since exception groups have no role
	// axis of their own — but now resolve per role too (see ExceptionResolver), so each group shows
	// its current preset plus the same 6-role breakdown executeFlagsList already prints for a
	// ROLE_BASED flag. A section with no groups is skipped entirely rather than printed empty.
	private static void sendExceptionGroupSection(
			CommandSourceStack source, Island island, List<ExceptionGroup> groups, ExceptionGroupCategory category, String title) {
		List<ExceptionGroup> inCategory = groups.stream().filter(group -> group.getCategory() == category).toList();
		if (inCategory.isEmpty()) {
			return;
		}

		IslandMessages.sectionTitle(source, title, ChatFormatting.GOLD);
		IslandRole[] roles = IslandRole.values();
		for (ExceptionGroup group : inCategory) {
			String configurableSuffix = group.isOwnerConfigurable() ? "" : " (solo gestionable por un admin)";
			String currentPreset = ExceptionResolver.currentPreset(island, group);

			MutableComponent header = Component.literal(group.getId()).withStyle(ChatFormatting.WHITE)
					.append(Component.literal(": preset " + currentPreset).withStyle(ChatFormatting.GRAY))
					.append(Component.literal(configurableSuffix).withStyle(ChatFormatting.GRAY));
			source.sendSuccess(() -> header, false);

			for (int i = 0; i < roles.length; i += 3) {
				int lineEnd = Math.min(i + 3, roles.length);
				MutableComponent line = Component.literal("  ");
				for (int j = i; j < lineEnd; j++) {
					IslandRole role = roles[j];
					boolean enabled = ExceptionResolver.isEnabledForRole(island, role, group);
					line.append(exceptionRoleValueText(role, enabled));
					if (j < lineEnd - 1) {
						line.append(Component.literal("   "));
					}
				}
				source.sendSuccess(() -> line, false);
			}
		}
	}

	private static Component exceptionRoleValueText(IslandRole role, boolean enabled) {
		ChatFormatting valueColor = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
		return Component.literal(role.name() + ": ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(enabled ? "ON" : "OFF").withStyle(valueColor));
	}

	private static int executeExceptionsPreset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String groupId = StringArgumentType.getString(ctx, "group");
		String preset = StringArgumentType.getString(ctx, "preset");

		Optional<ExceptionGroup> maybeGroup = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(groupId);
		if (maybeGroup.isEmpty()) {
			source.sendFailure(Component.literal("Grupo de excepción desconocido: " + groupId));
			return 0;
		}

		if (!maybeGroup.get().isOwnerConfigurable()) {
			source.sendFailure(Component.literal("El grupo " + groupId + " solo puede gestionarlo un administrador."));
			return 0;
		}

		ActionOutcome<Void> outcome = IslandActionService.applyExceptionGroupPreset(player.getUUID(), groupId, preset);
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.INVALID_EXCEPTION_PRESET.equals(outcome.reason())) {
				source.sendFailure(Component.literal("Preset inválido. Usa uno de: " + String.join(", ", FLAG_PRESET_NAMES) + "."));
			} else {
				source.sendFailure(Component.literal("Solo el propietario de la isla puede cambiar sus grupos de excepción."));
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal(groupId + " = preset " + preset), false);

		return 1;
	}

	private static int executeAdminExceptionsSetDefault(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		String groupId = StringArgumentType.getString(ctx, "group");
		String presetArg = StringArgumentType.getString(ctx, "preset");

		if (IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(groupId).isEmpty()) {
			source.sendFailure(Component.literal("Grupo de excepción desconocido: " + groupId));
			return 0;
		}

		Optional<FlagPreset> preset = FlagPreset.fromId(presetArg.toLowerCase(Locale.ROOT));
		if (preset.isEmpty()) {
			source.sendFailure(Component.literal("Preset inválido: " + presetArg + ". Usa uno de: " + String.join(", ", FLAG_PRESET_NAMES) + "."));
			return 0;
		}

		IslandCoreMod.SERVER_EXCEPTION_DEFAULTS.setDefault(groupId, preset.get());

		source.sendSuccess(() -> Component.literal(
				"Valor por defecto del servidor para el grupo " + groupId + " = preset " + preset.get().getId()), false);

		return 1;
	}

	private static int executeAdminSpawnExceptionsPreset(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		String groupId = StringArgumentType.getString(ctx, "group");
		String presetArg = StringArgumentType.getString(ctx, "preset");

		if (IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getGroup(groupId).isEmpty()) {
			source.sendFailure(Component.literal("Grupo de excepción desconocido: " + groupId));
			return 0;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		try {
			IslandCoreMod.ISLAND_REGISTRY.applyExceptionGroupPreset(maybeIsland.get().getIslandId(), groupId, presetArg.toLowerCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal("Preset inválido: " + presetArg + ". Usa uno de: " + String.join(", ", FLAG_PRESET_NAMES) + "."));
			return 0;
		}

		source.sendSuccess(() -> Component.literal(groupId + " = preset " + presetArg.toLowerCase(Locale.ROOT) + " en la isla de Spawn."), false);

		return 1;
	}

	private static CompletableFuture<Suggestions> suggestRoleBasedFlagIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		List<String> ids = FlagRegistry.all().stream()
				.filter(flag -> flag.getCategory() == FlagCategory.ROLE_BASED)
				.map(Flag::getId)
				.toList();
		return SharedSuggestionProvider.suggest(ids, builder);
	}

	private static CompletableFuture<Suggestions> suggestFlagIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		List<String> ids = FlagRegistry.all().stream().map(Flag::getId).toList();
		return SharedSuggestionProvider.suggest(ids, builder);
	}

	private static CompletableFuture<Suggestions> suggestExceptionGroupIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		List<String> ids = IslandCoreMod.EXCEPTION_GROUP_REGISTRY.getAllGroups().stream().map(ExceptionGroup::getId).toList();
		return SharedSuggestionProvider.suggest(ids, builder);
	}

	// Reads the "flag" argument already typed earlier in this same command (Brigadier resolves
	// arguments left-to-right, so it's guaranteed parsed by the time suggestions run for "value")
	// to offer only the vocabulary that flag's category actually accepts — allow/deny for
	// ISLAND_GLOBAL, the 4 presets for ROLE_BASED — instead of the full union of both regardless of
	// which flag was typed. Falls back to the full union for an unrecognized or not-yet-typed flag
	// id, so suggestions still show something reasonable while the player is still typing "flag".
	private static CompletableFuture<Suggestions> suggestFlagSetDefaultValues(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		String flagId = StringArgumentType.getString(ctx, "flag");
		List<String> names = FlagRegistry.get(flagId)
				.map(flag -> flag.getCategory() == FlagCategory.ISLAND_GLOBAL ? ALLOW_DENY_NAMES : FLAG_PRESET_NAMES)
				.orElse(FLAG_SET_DEFAULT_VALUE_NAMES);
		return SharedSuggestionProvider.suggest(names, builder);
	}

	private static int executeDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		ActionOutcome<Void> outcome = IslandActionService.requestDelete(player.getUUID());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else {
				source.sendFailure(Component.literal("Esta isla ya está en proceso de eliminación."));
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal(
				"¿Seguro que quieres borrar tu isla? Esta acción no se puede deshacer. "
						+ "Usa /island delete confirm en los próximos 30 segundos para confirmar."), false);

		return 1;
	}

	private static int executeDeleteConfirm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		ActionOutcome<Void> outcome = IslandActionService.confirmDelete(player.getUUID());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else {
				source.sendFailure(Component.literal(
						"No hay ninguna solicitud de borrado pendiente (o ha expirado). Usa /island delete primero."));
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal("Tu isla se está borrando..."), false);

		return 1;
	}

	private static int executeLimits(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		UUID playerUuid = player.getUUID();

		int maxSize = IslandCoreMod.PERMISSION_PROVIDER.getHighestSizeAllowed(playerUuid);
		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(playerUuid);

		String sizeLine;
		if (maybeIsland.isPresent()) {
			int currentSize = maybeIsland.get().getIslandSize();
			sizeLine = currentSize >= maxSize
					? "Ya tienes el tamaño máximo permitido (" + maxSize + ")."
					: "Tamaño máximo permitido: " + maxSize + " (tu isla actual: " + currentSize
							+ " — puedes ejecutar /island upgrade).";
		} else {
			sizeLine = "Tamaño máximo permitido: " + maxSize + ".";
		}

		String cooldownLine = IslandCoreMod.PERMISSION_PROVIDER.hasPermission(playerUuid, IslandPermissions.TELEPORT_COOLDOWN_BYPASS)
				? "No tienes ningún cooldown para /island home."
				: "Cooldown de /island home: " + IslandCoreMod.PERMISSION_PROVIDER.getHomeCooldownSeconds(playerUuid) + " segundos.";

		String message = sizeLine + "\n" + cooldownLine;
		source.sendSuccess(() -> Component.literal(message), false);

		return 1;
	}

	private static int executeInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		GameProfile targetProfile = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();

		ActionOutcome<Boolean> outcome = MembershipService.invite(player, targetProfile.getId(), source.getServer());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else if (ActionReason.ALREADY_OWNER.equals(outcome.reason())) {
				source.sendFailure(Component.literal("Ya eres el propietario de esta isla."));
			} else {
				source.sendFailure(Component.literal("Ese jugador ya es miembro de tu isla."));
			}
			return 0;
		}

		boolean targetOnline = outcome.data();
		if (targetOnline) {
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

		ActionOutcome<Island> outcome = MembershipService.acceptInvite(player, source.getServer());
		if (!outcome.success()) {
			source.sendFailure(Component.literal("No tienes ninguna invitación pendiente (o ha caducado)."));
			return 0;
		}

		source.sendSuccess(() -> Component.literal("¡Te has unido a la isla!"), false);

		return 1;
	}

	private static int executeTrust(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer executor = source.getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		ActionOutcome<Void> outcome = MembershipService.trust(executor, target.getUUID());
		if (!outcome.success()) {
			source.sendFailure(Component.literal("No tienes una isla."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendSuccess(() -> Component.literal(targetName + " ahora es copropietario de tu isla."), false);

		return 1;
	}

	private static int executeUntrust(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer executor = source.getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		String targetName = target.getGameProfile().getName();

		ActionOutcome<Void> outcome = MembershipService.untrust(executor, target.getUUID());
		if (!outcome.success()) {
			if (ActionReason.NOT_CO_OWNER.equals(outcome.reason())) {
				source.sendFailure(Component.literal(targetName + " no es copropietario de tu isla."));
			} else {
				source.sendFailure(Component.literal("No tienes una isla."));
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal(targetName + " ya no es copropietario de tu isla (sigue siendo miembro)."), false);

		return 1;
	}

	private static int executeAllyAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer executor = source.getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		ActionOutcome<Void> outcome = MembershipService.allyAdd(executor, target.getUUID());
		if (!outcome.success()) {
			source.sendFailure(Component.literal("No tienes una isla."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendSuccess(() -> Component.literal(targetName + " ahora es aliado de tu isla."), false);

		return 1;
	}

	private static int executeAllyRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer executor = source.getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		ActionOutcome<Void> outcome = MembershipService.allyRemove(executor, target.getUUID());
		if (!outcome.success()) {
			source.sendFailure(Component.literal("No tienes una isla."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendSuccess(() -> Component.literal(targetName + " ya no es aliado de tu isla."), false);

		return 1;
	}

	private static int executeKick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		GameProfile targetProfile = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();
		UUID targetUuid = targetProfile.getId();

		ActionOutcome<Void> outcome = MembershipService.kick(player, targetUuid, source.getServer());
		if (!outcome.success()) {
			if (ActionReason.NO_ISLAND.equals(outcome.reason())) {
				source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
			} else {
				source.sendFailure(Component.literal(targetProfile.getName() + " no es miembro de tu isla."));
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal(targetProfile.getName() + " ha sido expulsado de tu isla."), false);

		return 1;
	}

	private static CompletableFuture<Suggestions> suggestBiomes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		ServerPlayer player = ctx.getSource().getPlayer();
		if (player == null) {
			return builder.buildFuture();
		}

		Collection<ResourceLocation> availableBiomes =
				IslandCoreMod.BIOME_TIER_REGISTRY.getAvailableBiomes(player.getUUID(), IslandCoreMod.PERMISSION_PROVIDER);
		return SharedSuggestionProvider.suggestResource(availableBiomes, builder);
	}

	@SuppressWarnings("unchecked")
	private static int executeBiome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		ResourceLocation biomeId = ResourceLocationArgument.getId(ctx, "biome");

		ActionOutcome<Object> outcome = IslandActionService.changeBiome(player, biomeId, source.getServer());
		if (!outcome.success()) {
			switch (outcome.reason()) {
				case ActionReason.NO_ISLAND -> source.sendFailure(Component.literal("No tienes ninguna isla todavía."));
				case ActionReason.NOT_OWNER ->
						source.sendFailure(Component.literal("Solo el propietario de la isla puede cambiar su bioma."));
				case ActionReason.BIOME_NOT_FOUND -> source.sendFailure(Component.literal("El bioma " + biomeId + " no existe."));
				case ActionReason.BIOME_LOCKED -> {
					List<String> tierIds = (List<String>) outcome.data();
					if (tierIds == null || tierIds.isEmpty()) {
						source.sendFailure(Component.literal("El bioma " + biomeId + " no está disponible."));
					} else {
						source.sendFailure(Component.literal("No tienes acceso al bioma " + biomeId
								+ ". Se desbloquea con el/los tier(s): " + String.join(", ", tierIds) + "."));
					}
				}
				case ActionReason.COOLDOWN_ACTIVE -> {
					long remainingSeconds = (Long) outcome.data();
					String remaining = formatDuration(Duration.ofSeconds(remainingSeconds));
					source.sendFailure(Component.literal(
							"Todavía no puedes volver a cambiar el bioma de tu isla. Podrás hacerlo en " + remaining + "."));
				}
				case ActionReason.DIMENSION_UNAVAILABLE ->
						source.sendFailure(Component.literal("La dimensión de tu isla no está disponible ahora mismo."));
				default -> throw new IllegalStateException("Unhandled ActionReason from changeBiome: " + outcome.reason());
			}
			return 0;
		}

		source.sendSuccess(() -> Component.literal(
				"Cambiando el bioma de tu isla a " + biomeId + "... puede tardar unos segundos en islas grandes."), false);

		return 1;
	}

	private static String formatDuration(Duration duration) {
		long totalSeconds = Math.max(1, duration.getSeconds());
		long days = totalSeconds / 86400;
		long hours = (totalSeconds % 86400) / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

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

	private static int executeAdminListAll(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();

		Collection<Island> islands = IslandCoreMod.ISLAND_REGISTRY.getAllIslands();
		if (islands.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No islands exist yet."), false);
			return 0;
		}

		source.sendSuccess(() -> Component.literal("Islands (" + islands.size() + "):"), false);
		for (Island island : islands) {
			IslandMessages.sendIslandSummary(source, island);
		}

		return islands.size();
	}

	private static int executeAdminListPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(target.getUUID());
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal(target.getGameProfile().getName() + " no tiene ninguna isla."));
			return 0;
		}

		IslandMessages.sendIslandSummaryAdmin(source, maybeIsland.get());
		return 1;
	}

	private static int executeAdminSpawnCreate(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		int size = IntegerArgumentType.getInteger(ctx, "size");

		Island island;
		try {
			island = IslandCoreMod.ISLAND_REGISTRY.createSpawnIsland(ISLANDS_DIMENSION, size);
		} catch (IllegalStateException e) {
			source.sendFailure(Component.literal("The spawn island already exists."));
			return 0;
		}

		BlockPos center = island.getCenter();
		source.sendSuccess(() -> Component.literal("Created spawn island " + island.getIslandId()
				+ " at (" + center.getX() + ", " + center.getY() + ", " + center.getZ() + ")"), false);

		return 1;
	}

	private static int executeAdminSpawnResize(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		int newSize = IntegerArgumentType.getInteger(ctx, "size");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		try {
			IslandCoreMod.ISLAND_REGISTRY.resizeIsland(maybeIsland.get().getIslandId(), newSize);
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> Component.literal("Isla de Spawn ampliada a tamaño " + newSize + "."), false);

		return 1;
	}

	private static int executeAdminSpawnSetHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("La isla de Spawn todavía no existe. Usa /island admin spawn create primero."));
			return 0;
		}

		Island island = maybeIsland.get();
		boolean inIslandsDimension = player.level().dimension().equals(ISLANDS_DIMENSION);
		boolean withinBuiltIsland = island.getBounds().contains(player.blockPosition());

		if (!inIslandsDimension || !withinBuiltIsland) {
			source.sendFailure(Component.literal("El home de la isla de Spawn debe fijarse dentro de la parte ya construida de esa isla."));
			return 0;
		}

		BlockPos pos = player.blockPosition();
		IslandCoreMod.ISLAND_REGISTRY.updateHomeLocation(island.getIslandId(), pos);

		source.sendSuccess(() -> Component.literal("Home de la isla de Spawn actualizado a ("
				+ pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")."), false);

		return 1;
	}

	private static int executeAdminSpawnBuildProtection(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		boolean value = BoolArgumentType.getBool(ctx, "value");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		IslandCoreMod.ISLAND_REGISTRY.updateIslandSetting(maybeIsland.get().getIslandId(), IslandSetting.BUILD_PROTECTION, value);

		source.sendSuccess(() -> Component.literal("Protección de construcción de la isla de Spawn: " + value + "."), false);

		return 1;
	}

	private static int executeAdminSpawnTrust(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		MembershipService.trustOnIsland(maybeIsland.get(), target.getUUID());

		String targetName = target.getGameProfile().getName();
		source.sendSuccess(() -> Component.literal(targetName + " ahora puede construir siempre en la isla de Spawn."), false);

		return 1;
	}

	private static int executeAdminSpawnUntrust(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(Island.SERVER_OWNER_UUID);
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal("La isla de Spawn todavía no existe."));
			return 0;
		}

		MembershipService.untrustOnIsland(maybeIsland.get(), target.getUUID());

		String targetName = target.getGameProfile().getName();
		source.sendSuccess(() -> Component.literal(targetName + " ya no tiene acceso especial en la isla de Spawn."), false);

		return 1;
	}

	private static int executeAdminDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer admin = source.getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(target.getUUID());
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal(target.getGameProfile().getName() + " no tiene ninguna isla."));
			return 0;
		}

		try {
			IslandCoreMod.DELETION_SERVICE.requestDeletion(maybeIsland.get().getIslandId(), admin.getUUID());
		} catch (IllegalArgumentException | IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendSuccess(() -> Component.literal("¿Seguro que quieres borrar la isla de " + targetName
				+ "? Usa /island admin delete " + targetName + " confirm en los próximos 30 segundos."), false);

		return 1;
	}

	private static int executeAdminDeleteConfirm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer admin = source.getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandByOwner(target.getUUID());
		if (maybeIsland.isEmpty()) {
			source.sendFailure(Component.literal(target.getGameProfile().getName() + " no tiene ninguna isla."));
			return 0;
		}

		boolean confirmed = IslandCoreMod.DELETION_SERVICE.confirmDeletion(maybeIsland.get().getIslandId(), admin.getUUID());
		if (!confirmed) {
			source.sendFailure(Component.literal("No hay ninguna solicitud de borrado pendiente (o ha expirado)."));
			return 0;
		}

		String targetName = target.getGameProfile().getName();
		source.sendSuccess(() -> Component.literal("La isla de " + targetName + " se está borrando..."), false);

		return 1;
	}

}
