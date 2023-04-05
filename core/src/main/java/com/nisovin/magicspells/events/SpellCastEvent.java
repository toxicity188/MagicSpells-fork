package com.nisovin.magicspells.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.Spell.SpellCastState;
/**
 * The event that is called whenever a player attempts to cast a spell. 
 * This event is called just before the effects of the spell are performed.
 * Cancelling this event will prevent the spell from casting.
 *
 */
public class SpellCastEvent extends SpellEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private SpellCastState state;
    private boolean stateChanged;
	private float cooldown;
	private SpellReagents reagents;
	private boolean reagentsChanged;
	private float power;
	private int castTime;
	private String[] args;
	private boolean cancelled = false;
	
	public SpellCastEvent(Spell spell, LivingEntity caster, SpellCastState state, float power, String[] args, float cooldown, SpellReagents reagents, int castTime) {
		super(spell, caster);
		this.state = state;
		this.cooldown = cooldown;
		this.reagents = reagents;
		this.power = power;
		this.castTime = castTime;
		this.args = args;
		stateChanged = false;
		reagentsChanged = false;
	}
	
	/**
	 * Gets the current spell cast state.
	 * @return the spell cast state
	 */
	public SpellCastState getSpellCastState() {
		return state;
	}
	
	/**
	 * Changes the spell cast state.
	 * @param state the new spell cast state
	 */
	public void setSpellCastState(SpellCastState state) {
		this.state = state;
		stateChanged = true;
	}
	
	/**
	 * Checks whether the spell cast state has been changed.
	 * @return true if it has been changed
	 */
	public boolean hasSpellCastStateChanged() {
		return stateChanged;
	}
	
	/**
	 * Gets the cooldown that will be triggered after the spell is cast.
	 * @return the cooldown
	 */
	public float getCooldown() {
		return cooldown;
	}
	
	/**
	 * Sets the cooldown that will be triggered after the spell is cast.
	 * @param cooldown the cooldown to set
	 */
	public void setCooldown(float cooldown) {
		this.cooldown = cooldown;
	}
	
	/**
	 * Gets the reagents that will be charged after the spell is cast. This can be modified.
	 * @return the reagents
	 */
	public SpellReagents getReagents() {
		return reagents;
	}
	
	/**
	 * Changes the spell's required cast reagents.
	 * @param reagents the new reagents
	 */
	public void setReagents(SpellReagents reagents) {
		this.reagents = reagents;
		reagentsChanged = true;
	}
	
	/**
	 * Gets whether the spell's reagents have been changed by this event.
	 * @return true if reagents are changed
	 */
	public boolean haveReagentsChanged() {
		return reagentsChanged;
	}
	
	/**
	 * Sets whether the reagents have been changed. If a plugin changes the reagents list,
	 * this should be called and set to true.
	 * @param reagentsChanged whether reagents have been changed
	 */
	public void setReagentsChanged(boolean reagentsChanged) {
		this.reagentsChanged = reagentsChanged;
	}
	
	/**
	 * Gets the current power level of the spell. Spells start at a power level of 1.0.
	 * @return the power level
	 */
	public float getPower() {
		return power;
	}
	
	/**
	 * Sets the power level for the spell being cast.
	 * @param power the power level
	 */
	public void setPower(float power) {
		this.power = power;
	}
	
	/**
	 * Increases the power lever for the spell being cast by the given multiplier.
	 * @param power the power level multiplier
	 */
	public void increasePower(float power) {
		this.power *= power;
	}
	
	/**
	 * Gets the cast time for this spell cast, in server ticks.
	 * @return the cast time
	 */
	public int getCastTime() {
		return castTime;
	}
	
	/**
	 * Sets the cast time for this spell cast, in server ticks.
	 * @param castTime the new cast time
	 */
	public void setCastTime(int castTime) {
		this.castTime = castTime;
	}
	
	/**
	 * Gets the arguments passed to the spell if the spell was cast by command.
	 * @return the args, or null if there were none
	 */
	public String[] getSpellArgs() {
		return args;
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
