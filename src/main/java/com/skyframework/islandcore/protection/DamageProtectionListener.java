package com.skyframework.islandcore.protection;

import com.skyframework.islandcore.IslandCoreMod;
import com.skyframework.islandcore.api.island.Island;
import com.skyframework.islandcore.protection.flag.FlagRegistry;
import com.skyframework.islandcore.protection.flag.FlagResolver;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

// Called from IslandCoreMod's second LivingIncomingDamageEvent listener.
//
// Deliberately asymmetric between the two toggles (explicit design decision, not an oversight):
//   - MOB_DAMAGE (at least one side isn't a player): an attacking player with the ENTITIES
//     permission always gets to attack/manage their own animals or defend against mobs,
//     regardless of MOB_DAMAGE — this isn't "outside damage" being let in, it's the owner/a
//     trusted member acting legitimately on the island they already have rights on.
//   - PVP_DAMAGE (both sides are players): NO exception, not even for the owner or someone with
//     ENTITIES. The outcome depends solely on the island's resolved PVP_DAMAGE value — pvp=false
//     means nobody can hit anybody there, pvp=true means anybody can hit anybody. This makes PVP
//     symmetric: the owner can't rely on ENTITIES to sidestep their own PVP setting.
public final class DamageProtectionListener {

	private static final ResourceKey<Level> ISLANDS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("islandcore", "islands"));

	private DamageProtectionListener() {
	}

	public static boolean isDamageAllowed(Level world, LivingEntity victim, DamageSource source) {
		if (!world.dimension().equals(ISLANDS_DIMENSION)) {
			return true;
		}

		Optional<Island> maybeIsland = IslandCoreMod.ISLAND_REGISTRY.getIslandAt(victim.blockPosition());
		if (maybeIsland.isEmpty()) {
			// Unclaimed zone within the dimension: nothing to protect there.
			return true;
		}

		Island island = maybeIsland.get();

		Entity attacker = source.getEntity();
		if (attacker == null) {
			// DamageSources.magic() (used by periodic harmful-status-effect ticks, e.g. poison from a
			// thrown potion) carries no attacker at all, unlike an instant potion effect's
			// indirectMagic() — see StatusEffectSourceTracker's class doc for the full picture. Check
			// the tracker before falling back to "no attacker, not part of this system" so PVP_DAMAGE
			// still blocks this the same way it blocks a direct hit.
			if (victim instanceof Player) {
				Optional<UUID> hostileEffectAttacker = StatusEffectSourceTracker.getHostileEffectSource(victim.getUUID());
				if (hostileEffectAttacker.isPresent()) {
					return FlagResolver.resolveGlobal(island, FlagRegistry.PVP_DAMAGE);
				}
			}
			// No attacker and no tracked hostile-effect source: fall damage, lava, drowning,
			// starvation, etc. Not part of this system.
			return true;
		}

		if (attacker instanceof Player && victim instanceof Player) {
			// PVP is symmetric: no ENTITIES bypass here, not even for the owner — see class javadoc.
			return FlagResolver.resolveGlobal(island, FlagRegistry.PVP_DAMAGE);
		}

		// At least one side is not a player: an attacking player with ENTITIES may always
		// attack/manage their own animals or defend against mobs, regardless of MOB_DAMAGE.
		if (attacker instanceof Player attackerPlayer
				&& FlagResolver.resolveForPlayer(island, attackerPlayer.getUUID(), FlagRegistry.ENTITIES)) {
			return true;
		}

		return FlagResolver.resolveGlobal(island, FlagRegistry.MOB_DAMAGE);
	}
}
