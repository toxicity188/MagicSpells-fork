package com.nisovin.magicspells.spelleffects;

import org.bukkit.entity.Entity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

public class EffectTracker implements Runnable {

	Entity entity;
	BuffSpell buffSpell;
	SpellEffect effect;
	SpellEffectActiveChecker checker;

	int effectTrackerTaskId;

	EffectTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect) {
		this.entity = entity;
		this.checker = checker;
		this.effect = effect;

		effectTrackerTaskId = MagicSpells.scheduleRepeatingTask(this, 0, effect.getEffectInterval());
	}

	public Entity getEntity() {
		return entity;
	}

	public BuffSpell getBuffSpell() {
		return buffSpell;
	}

	public SpellEffect getEffect() {
		return effect;
	}

	public SpellEffectActiveChecker getChecker() {
		return checker;
	}

	public int getEffectTrackerTaskId() {
		return effectTrackerTaskId;
	}

	public void setBuffSpell(BuffSpell spell) {
		buffSpell = spell;
	}

	@Override
	public void run() {

	}

	public void stop() {
		MagicSpells.cancelTask(effectTrackerTaskId);
		entity = null;
	}

	public void unregister() {
		if (buffSpell != null) buffSpell.getEffectTrackers().remove(this);
	}

}
