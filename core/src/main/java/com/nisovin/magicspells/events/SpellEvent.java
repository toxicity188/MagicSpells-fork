package com.nisovin.magicspells.events;

import org.bukkit.event.Event;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;

public abstract class SpellEvent extends Event implements IMagicSpellsCompatEvent {

	protected Spell spell;
	protected LivingEntity caster;
	
	public SpellEvent(Spell spell, LivingEntity caster) {
		this.spell = spell;
		this.caster = caster;
	}
	
	/**
	 * Gets the spell involved in the event.
	 * @return the spell
	 */
	public Spell getSpell() {
		return spell;
	}
	
	/**
	 * Gets the player casting the spell.
	 * @return the casting player
	 */
	public LivingEntity getCaster() {
		return caster;
	}
	
}
