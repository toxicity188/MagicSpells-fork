package com.nisovin.magicspells.events;

import com.nisovin.magicspells.power.Power;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;

/**
 * This event is fired whenever a TargetedSpell is trying to target an entity.
 * Cancelling this event will prevent the spell from targeting the entity.
 *
 */
public class SpellTargetEvent extends SpellEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

	private LivingEntity target;
	private Power power;
	private boolean cancelled = false;
	
	public SpellTargetEvent(Spell spell, LivingEntity caster, LivingEntity target, Power power) {
		super(spell, caster);
		this.target = target;
		this.power = power;
	}
	
	/**
	 * Gets the living entity that is being targeted by the spell.
	 * @return the targeted living entity
	 */
	public LivingEntity getTarget() {
		return target;
	}
	
	/**
	 * Sets the spell's target to the provided living entity.
	 * @param target the new target
	 */
	public void setTarget(LivingEntity target) {
		this.target = target;
	}
	
	/**
	 * Gets the current power level of the spell. Spells start at a power level of 1.0.
	 * @return the power level
	 */
	public Power getPower() {
		return power;
	}
	
	/**
	 * Sets the power level for the spell being cast.
	 * @param power the power level
	 */
	public void setPower(Power power) {
		this.power = power;
	}
	
	/**
	 * Increases the power lever for the spell being cast by the given multiplier.
	 * @param power the power level multiplier
	 */
	public void increasePower(Power power) {
		this.power = this.power.multiply(power);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;		
	}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
	
}
