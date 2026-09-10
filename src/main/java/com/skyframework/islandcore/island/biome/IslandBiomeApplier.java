package com.skyframework.islandcore.island.biome;

import com.skyframework.islandcore.island.model.IslandBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

// PORTED FROM FABRIC — HIGH RISK, NOT YET VERIFIED AGAINST A RUNNING NEOFORGE SERVER. This class
// pokes at deep, semi-internal chunk/biome-resend machinery (the same mechanism vanilla's own
// /fillbiome command uses) that Mojmap may expose under slightly different field/method shapes
// than assumed here (ChunkMap's resend-biome method name, the exact Climate.Sampler accessor
// chain). Everything else in this sprint's port compiled clean against the real NeoForge jar
// first; this file is the one exception where the translation is a best-effort guess pending a
// real compile + in-game biome-change test.
//
// Sets the real Minecraft biome over an island's plot, chunk column by chunk column, budgeted
// across ticks the same way IslandDeletionServiceImpl budgets its block-clearing. On top of what
// vanilla does, each touched chunk is also resent as a full chunk packet to every player currently
// watching it, so grass/foliage/water color refreshes immediately instead of needing a manual F3+A.
public class IslandBiomeApplier {

	// Adjustable: how many chunk columns to process per server tick.
	private static final int CHUNKS_PER_TICK = 3;

	private final Deque<PendingBiomeJob> jobs = new ArrayDeque<>();

	// onInitialize() runs before the MinecraftServer exists, so the reference is captured lazily on server start.
	private MinecraftServer server;

	public IslandBiomeApplier() {
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> this.server = event.getServer());
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tick());
	}

	public void enqueue(ServerLevel world, IslandBounds bounds, Holder<Biome> targetBiome, UUID requestedBy) {
		jobs.add(new PendingBiomeJob(world, bounds, targetBiome, requestedBy, true, null));
	}

	// Used by IslandDeletionServiceImpl to reset a deleted island's plot back to void: notifyPlayer
	// is false there (the "your biome change finished" message would be confusing when the island
	// is being deleted, not changed), and onFinished lets the deletion service chain its final
	// cleanup (spatial index/grid/storage) only once the plot's biome has actually been reset —
	// same tick-budget/chunk-resend mechanism as the 4-arg overload above, nothing duplicated.
	public void enqueue(ServerLevel world, IslandBounds bounds, Holder<Biome> targetBiome, UUID requestedBy,
			boolean notifyPlayer, Runnable onFinished) {
		jobs.add(new PendingBiomeJob(world, bounds, targetBiome, requestedBy, notifyPlayer, onFinished));
	}

	private void tick() {
		int budget = CHUNKS_PER_TICK;

		while (budget > 0) {
			PendingBiomeJob job = jobs.peek();
			if (job == null) {
				return;
			}

			while (budget > 0 && !job.remainingColumns.isEmpty()) {
				processColumn(job, job.remainingColumns.poll());
				budget--;
			}

			if (job.remainingColumns.isEmpty()) {
				jobs.poll();
				if (job.notifyPlayer) {
					notifyFinished(job);
				}
				if (job.onFinished != null) {
					job.onFinished.run();
				}
			}
		}
	}

	private void processColumn(PendingBiomeJob job, ChunkPos chunkPos) {
		LevelChunk chunk = job.world.getChunk(chunkPos.x, chunkPos.z);

		BiomeResolver resolver = (biomeX, biomeY, biomeZ, sampler) -> {
			BlockPos blockPos = new BlockPos(QuartPos.toBlock(biomeX), QuartPos.toBlock(biomeY), QuartPos.toBlock(biomeZ));
			if (job.bounds.contains(blockPos)) {
				return job.targetBiome;
			}
			return chunk.getNoiseBiome(biomeX, biomeY, biomeZ);
		};

		chunk.fillBiomesFromNoise(resolver, job.sampler);
		chunk.setUnsaved(true);

		ChunkMap chunkMap = job.world.getChunkSource().chunkMap;
		chunkMap.resendBiomesForChunks(List.of(chunk));

		ClientboundLevelChunkWithLightPacket fullChunkPacket =
				new ClientboundLevelChunkWithLightPacket(chunk, job.world.getLightEngine(), null, null);
		for (ServerPlayer watcher : chunkMap.getPlayers(chunkPos, false)) {
			watcher.connection.send(fullChunkPacket);
		}
	}

	private void notifyFinished(PendingBiomeJob job) {
		if (server == null) {
			return;
		}

		ServerPlayer player = server.getPlayerList().getPlayer(job.requestedBy);
		if (player != null) {
			player.sendSystemMessage(Component.literal("El cambio de bioma de tu isla ha terminado."));
		}
	}

	private static Deque<ChunkPos> collectColumns(IslandBounds bounds) {
		int minChunkX = SectionPos.blockToSectionCoord(bounds.min().getX());
		int maxChunkX = SectionPos.blockToSectionCoord(bounds.max().getX());
		int minChunkZ = SectionPos.blockToSectionCoord(bounds.min().getZ());
		int maxChunkZ = SectionPos.blockToSectionCoord(bounds.max().getZ());

		Deque<ChunkPos> columns = new ArrayDeque<>();
		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				columns.add(new ChunkPos(x, z));
			}
		}
		return columns;
	}

	private static final class PendingBiomeJob {
		final ServerLevel world;
		final IslandBounds bounds;
		final Holder<Biome> targetBiome;
		final UUID requestedBy;
		final boolean notifyPlayer;
		final Runnable onFinished;
		final Climate.Sampler sampler;
		final Deque<ChunkPos> remainingColumns;

		PendingBiomeJob(ServerLevel world, IslandBounds bounds, Holder<Biome> targetBiome, UUID requestedBy,
				boolean notifyPlayer, Runnable onFinished) {
			this.world = world;
			this.bounds = bounds;
			this.targetBiome = targetBiome;
			this.requestedBy = requestedBy;
			this.notifyPlayer = notifyPlayer;
			this.onFinished = onFinished;
			RandomState randomState = world.getChunkSource().randomState();
			this.sampler = randomState.sampler();
			this.remainingColumns = collectColumns(bounds);
		}
	}
}
