package com.skyframework.islandcore.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.dimension.model.DimensionDefinition;
import com.skyframework.islandcore.dimension.model.DimensionGeneratorStyle;
import com.skyframework.islandcore.dimension.vanilla.PendingVanillaReset;
import com.skyframework.islandcore.dimension.vanilla.VanillaResetService;
import com.skyframework.islandcore.util.ServerLang;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

// Admin-only ("/dimension", requires permission level 2) commands for the Dimension Manager.
public class DimensionCommand {

	private DimensionCommand() {
	}

	public static void register() {
		NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
				event.getDispatcher().register(Commands.literal("dimension")
						.requires(source -> source.hasPermission(2))
						.then(Commands.literal("create")
								.then(Commands.argument("id", StringArgumentType.word())
										.then(Commands.argument("displayName", StringArgumentType.string())
												.then(Commands.argument("style", StringArgumentType.word())
														.suggests(DimensionCommand::suggestStyles)
														.executes(ctx -> executeCreate(ctx, null))
														.then(Commands.argument("seed", LongArgumentType.longArg())
																.executes(ctx -> executeCreate(ctx, LongArgumentType.getLong(ctx, "seed"))))))))
						.then(Commands.literal("list")
								.executes(DimensionCommand::executeList))
						.then(Commands.literal("info")
								.then(Commands.argument("id", StringArgumentType.word())
										.suggests(DimensionCommand::suggestExistingIds)
										.executes(DimensionCommand::executeInfo)))
						.then(Commands.literal("delete")
								.then(Commands.argument("id", StringArgumentType.word())
										.suggests(DimensionCommand::suggestExistingIds)
										.executes(DimensionCommand::executeDelete)
										.then(Commands.literal("confirm")
												.executes(DimensionCommand::executeDeleteConfirm))))
						.then(Commands.literal("regenerate")
								.then(Commands.argument("id", StringArgumentType.word())
										.suggests(DimensionCommand::suggestExistingIds)
										.executes(ctx -> executeRegenerate(ctx, null))
										.then(Commands.argument("seed", LongArgumentType.longArg())
												.executes(ctx -> executeRegenerate(ctx, LongArgumentType.getLong(ctx, "seed"))))
										.then(Commands.literal("confirm")
												.executes(DimensionCommand::executeRegenerateConfirm))))
						.then(Commands.literal("vanilla")
								.then(Commands.literal("regenerate")
										.then(Commands.argument("dimensionKey", StringArgumentType.word())
												.suggests(DimensionCommand::suggestVanillaDimensionKeys)
												.executes(ctx -> executeVanillaRegenerate(ctx, null))
												.then(Commands.argument("seed", LongArgumentType.longArg())
														.executes(ctx -> executeVanillaRegenerate(ctx, LongArgumentType.getLong(ctx, "seed"))))
												.then(Commands.literal("confirm")
														.executes(DimensionCommand::executeVanillaRegenerateConfirm))))
								.then(Commands.literal("cancel")
										.then(Commands.argument("dimensionKey", StringArgumentType.word())
												.suggests(DimensionCommand::suggestVanillaDimensionKeys)
												.executes(DimensionCommand::executeVanillaCancel)))
								.then(Commands.literal("list")
										.executes(DimensionCommand::executeVanillaList)))
				)
		);
	}

	private static int executeCreate(CommandContext<CommandSourceStack> ctx, Long explicitSeed) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		String idPath = StringArgumentType.getString(ctx, "id");
		String displayName = StringArgumentType.getString(ctx, "displayName");
		String styleArg = StringArgumentType.getString(ctx, "style");

		DimensionGeneratorStyle style;
		try {
			style = DimensionGeneratorStyle.valueOf(styleArg.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal("Estilo desconocido: " + styleArg
					+ ". Usa uno de: " + Arrays.toString(DimensionGeneratorStyle.values())));
			return 0;
		}

		ResourceLocation id;
		try {
			id = ResourceLocation.fromNamespaceAndPath("islandcore", idPath);
		} catch (RuntimeException e) {
			source.sendFailure(Component.literal("Id inválido: \"" + idPath
					+ "\" (solo minúsculas, números, '_', '-' y '/')."));
			return 0;
		}

		long seed = explicitSeed != null ? explicitSeed : new Random().nextLong();

		DimensionDefinition dimension;
		try {
			dimension = IslandCoreMod.DIMENSION_REGISTRY.createDimension(id, displayName, style, seed);
		} catch (IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> Component.literal("Dimensión creada: " + dimension.getId()
				+ " (\"" + dimension.getDisplayName() + "\", estilo " + dimension.getGeneratorStyle()
				+ ", semilla " + dimension.getSeed() + ")."), false);

		return 1;
	}

	private static int executeList(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		Collection<DimensionDefinition> dimensions = IslandCoreMod.DIMENSION_REGISTRY.getAllDimensions();

		if (dimensions.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No hay dimensiones gestionadas todavía."), false);
			return 0;
		}

		source.sendSuccess(() -> Component.literal("Dimensiones (" + dimensions.size() + "):"), false);
		for (DimensionDefinition dimension : dimensions) {
			source.sendSuccess(() -> Component.literal("- " + dimension.getId() + " \"" + dimension.getDisplayName() + "\""
					+ " estilo=" + dimension.getGeneratorStyle()
					+ " semilla=" + dimension.getSeed()
					+ " estado=" + dimension.getState()), false);
		}

		return dimensions.size();
	}

	private static int executeInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		String idPath = StringArgumentType.getString(ctx, "id");
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("islandcore", idPath);

		Optional<DimensionDefinition> maybeDimension = IslandCoreMod.DIMENSION_REGISTRY.getDimension(id);
		if (maybeDimension.isEmpty()) {
			source.sendFailure(Component.literal("No existe ninguna dimensión gestionada con id " + id + "."));
			return 0;
		}

		DimensionDefinition dimension = maybeDimension.get();
		source.sendSuccess(() -> Component.literal("id=" + dimension.getId()), false);
		source.sendSuccess(() -> Component.literal("displayName=" + dimension.getDisplayName()), false);
		source.sendSuccess(() -> Component.literal("generatorStyle=" + dimension.getGeneratorStyle()), false);
		source.sendSuccess(() -> Component.literal("seed=" + dimension.getSeed()), false);
		source.sendSuccess(() -> Component.literal("state=" + dimension.getState()), false);
		source.sendSuccess(() -> Component.literal("createdAt=" + dimension.getCreatedAt()), false);
		source.sendSuccess(() -> Component.literal("updatedAt=" + dimension.getUpdatedAt()), false);

		return 1;
	}

	private static int executeDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("islandcore", StringArgumentType.getString(ctx, "id"));

		try {
			IslandCoreMod.DIMENSION_REGISTRY.requestDeletion(id, player.getUUID());
		} catch (IllegalArgumentException | IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player, "¿Seguro que quieres borrar la dimensión " + id
				+ "? Esta acción no se puede deshacer. Usa /dimension delete " + id.getPath()
				+ " confirm en los próximos 30 segundos para confirmar.",
				"Are you sure you want to delete dimension " + id
				+ "? This action cannot be undone. Use /dimension delete " + id.getPath()
				+ " confirm within the next 30 seconds to confirm."), false);

		return 1;
	}

	private static int executeDeleteConfirm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("islandcore", StringArgumentType.getString(ctx, "id"));

		boolean confirmed = IslandCoreMod.DIMENSION_REGISTRY.confirmDeletion(id, player.getUUID());
		if (!confirmed) {
			source.sendFailure(ServerLang.of(player, "No hay ninguna solicitud de borrado pendiente para " + id + " (o ha expirado).",
					"There is no pending deletion request for " + id + " (or it has expired)."));
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player, "La dimensión " + id + " se está borrando...",
				"Dimension " + id + " is being deleted..."), false);

		return 1;
	}

	private static int executeRegenerate(CommandContext<CommandSourceStack> ctx, Long explicitSeed) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("islandcore", StringArgumentType.getString(ctx, "id"));

		long newSeed = explicitSeed != null ? explicitSeed : new Random().nextLong();

		try {
			IslandCoreMod.DIMENSION_REGISTRY.requestRegeneration(id, player.getUUID(), newSeed);
		} catch (IllegalArgumentException | IllegalStateException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player, "¿Seguro que quieres regenerar la dimensión " + id
				+ " con la semilla " + newSeed + "? Todo lo construido en ella se perderá. Usa /dimension regenerate "
				+ id.getPath() + " confirm en los próximos 30 segundos para confirmar.",
				"Are you sure you want to regenerate dimension " + id
				+ " with seed " + newSeed + "? Everything built in it will be lost. Use /dimension regenerate "
				+ id.getPath() + " confirm within the next 30 seconds to confirm."), false);

		return 1;
	}

	private static int executeRegenerateConfirm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath("islandcore", StringArgumentType.getString(ctx, "id"));

		boolean confirmed = IslandCoreMod.DIMENSION_REGISTRY.confirmRegeneration(id, player.getUUID());
		if (!confirmed) {
			source.sendFailure(ServerLang.of(player, "No hay ninguna solicitud de regeneración pendiente para " + id + " (o ha expirado).",
					"There is no pending regeneration request for " + id + " (or it has expired)."));
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player, "La dimensión " + id + " se está regenerando...",
				"Dimension " + id + " is being regenerated..."), false);

		return 1;
	}

	private static int executeVanillaRegenerate(CommandContext<CommandSourceStack> ctx, Long explicitSeed) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String dimensionKey = StringArgumentType.getString(ctx, "dimensionKey");

		try {
			IslandCoreMod.VANILLA_RESET_SERVICE.requestReset(dimensionKey, player.getUUID(), explicitSeed);
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player, "¿Seguro que quieres resetear la dimensión vanilla " + dimensionKey
				+ "? Esta acción no se puede deshacer. Usa /dimension vanilla regenerate " + dimensionKey
				+ " confirm en los próximos 30 segundos para confirmar.",
				"Are you sure you want to reset the vanilla dimension " + dimensionKey
				+ "? This action cannot be undone. Use /dimension vanilla regenerate " + dimensionKey
				+ " confirm within the next 30 seconds to confirm."), false);

		return 1;
	}

	private static int executeVanillaRegenerateConfirm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String dimensionKey = StringArgumentType.getString(ctx, "dimensionKey");

		boolean confirmed;
		try {
			confirmed = IslandCoreMod.VANILLA_RESET_SERVICE.confirmReset(dimensionKey, player.getUUID());
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		if (!confirmed) {
			source.sendFailure(ServerLang.of(player, "No hay ninguna solicitud de reseteo pendiente para " + dimensionKey + " (o ha expirado).",
					"There is no pending reset request for " + dimensionKey + " (or it has expired)."));
			return 0;
		}

		source.sendSuccess(() -> ServerLang.of(player, "Reseteo de " + dimensionKey + " añadido a la cola de reseteos pendientes"
				+ " (junto a cualquier otro ya solicitado, si lo hay). Se aplicará en el PRÓXIMO reinicio del servidor:"
				+ " el mod no puede reiniciarlo por sí mismo, así que debes pararlo y volver a arrancarlo tú mismo"
				+ " cuando quieras que se apliquen todos los reseteos en cola.",
				"Reset of " + dimensionKey + " added to the pending reset queue"
				+ " (along with any others already requested, if any). It will be applied on the NEXT server restart:"
				+ " the mod cannot restart itself, so you must stop it and start it back up yourself"
				+ " whenever you want all queued resets to be applied."), false);

		return 1;
	}

	private static int executeVanillaCancel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		String dimensionKey = StringArgumentType.getString(ctx, "dimensionKey");

		boolean cancelled;
		try {
			cancelled = IslandCoreMod.VANILLA_RESET_SERVICE.cancelPendingReset(dimensionKey);
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal(e.getMessage()));
			return 0;
		}

		if (!cancelled) {
			source.sendFailure(Component.literal("No hay ninguna solicitud de reseteo pendiente para " + dimensionKey + " en la cola."));
			return 0;
		}

		source.sendSuccess(() -> Component.literal("Reseteo pendiente de " + dimensionKey
				+ " cancelado. No se aplicará en el próximo reinicio."), false);

		return 1;
	}

	private static int executeVanillaList(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		List<PendingVanillaReset> queue = IslandCoreMod.VANILLA_RESET_SERVICE.listPendingResets();

		if (queue.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No hay ningún reseteo vanilla pendiente en la cola."), false);
			return 0;
		}

		source.sendSuccess(() -> Component.literal("Reseteos vanilla pendientes (" + queue.size() + "):"), false);
		for (PendingVanillaReset pending : queue) {
			String seedText = pending.getSeed() != null ? String.valueOf(pending.getSeed()) : "mantiene la actual";
			source.sendSuccess(() -> Component.literal("- ")
					.append(vanillaDimensionDisplayName(pending.getDimensionKey()))
					.append(Component.literal(" | semilla=" + seedText + " | solicitado por " + pending.getRequestedBy())), false);
		}

		return queue.size();
	}

	// Bold + a color of its own per dimension, matching IslandMessages' labeled-value pattern
	// (colored/bold piece via .formatted(...), plain rest of the line appended alongside it).
	private static Component vanillaDimensionDisplayName(String dimensionKey) {
		return switch (dimensionKey) {
			case "overworld" -> Component.literal("Overworld").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN);
			case "nether" -> Component.literal("Nether").withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
			case "end" -> Component.literal("End").withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE);
			default -> Component.literal(dimensionKey);
		};
	}

	private static CompletableFuture<Suggestions> suggestVanillaDimensionKeys(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(VanillaResetService.VALID_DIMENSION_KEYS, builder);
	}

	private static CompletableFuture<Suggestions> suggestStyles(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		List<String> names = Arrays.stream(DimensionGeneratorStyle.values()).map(Enum::name).toList();
		return SharedSuggestionProvider.suggest(names, builder);
	}

	private static CompletableFuture<Suggestions> suggestExistingIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		List<String> paths = IslandCoreMod.DIMENSION_REGISTRY.getAllDimensions().stream()
				.map(dimension -> dimension.getId().getPath())
				.toList();
		return SharedSuggestionProvider.suggest(paths, builder);
	}
}
