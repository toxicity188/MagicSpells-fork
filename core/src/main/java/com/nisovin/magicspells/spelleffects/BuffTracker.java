package com.nisovin.magicspells.spelleffects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

public class BuffTracker extends EffectTracker implements Runnable {

	BuffTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect) {
		super(entity, checker, effect);
	}

	@Override
	public void run() {
		if (!entity.isValid() || !checker.isActive(entity) || effect == null) {
			stop();
			return;
		}

		if (entity instanceof LivingEntity && !effect.getModifiers().check((LivingEntity) entity)) return;

		effect.playEffect(entity);
	}

	@Override
	public void stop() {
		super.stop();
	}

}
