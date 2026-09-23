package com.skyframework.islandcore.mixin;

import com.skyframework.islandcore.protection.StatusEffectSourceTracker;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// NEOFORGE PORT: kept as a Mixin, same as the Fabric original. LivingEntity#addEffect(MobEffectInstance,
// Entity) (Yarn: addStatusEffect) is the ONLY point in vanilla where a harmful effect's applying
// entity is still known — MobEffectEvent.Added does expose the source, but only fires once the effect
// has actually been applied/merged, whereas the Fabric mixin records at HEAD (before any early return),
// and StatusEffectSourceTracker relies on that exact timing (see its class doc). Signature verified
// against the neoforge-21.1.250 jar: `public boolean addEffect(MobEffectInstance, Entity)`.
@Mixin(LivingEntity.class)
public abstract class LivingEntityStatusEffectSourceMixin {

	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"))
	private void islandcore$recordHostileEffectSource(
			MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
		if (!(source instanceof Player) || effect.getEffect().value().isBeneficial()) {
			return;
		}

		LivingEntity self = (LivingEntity) (Object) this;
		StatusEffectSourceTracker.record(self.getUUID(), source.getUUID(), effect.getDuration());
	}
}
