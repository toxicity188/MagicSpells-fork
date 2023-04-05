package com.nisovin.magicspells.events;

import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.PostCastAction;

/** 
 * The event that is called whenever a player casts a spell. 
 * This event is called after the spell is done and everything has been handled.
 *
 */
public class SpellCastedEvent extends SpellEvent {
	
    private static final HandlerList handlers = new HandlerList();

    private SpellCastState state;
	private float cooldown;
	private SpellReagents reagents;
	private float power;
	private String[] args;
	private PostCastAction action;
	
	public SpellCastedEvent(Spell spell, LivingEntity caster, SpellCastState state, float power, String[] args, float cooldown, SpellReagents reagents, PostCastAction action) {
		super(spell, caster);
		this.state = state;
		this.cooldown = cooldown;
		this.reagents = reagents;
		this.power = power;
		this.args = args;
		this.action = action;
	}
	
	/**
	 * Gets the current spell cast state.
	 * @return the spell cast state
	 */
	public SpellCastState getSpellCastState() {
		return state;
	}
	
	/**
	 * Gets the cooldown that was triggered by the spell.
	 * @return the cooldown
	 */
	public float getCooldown() {
		return cooldown;
	}
	
	/**
	 * Gets the reagents that were charged.
	 * @return the reagents
	 */
	public SpellReagents getReagents() {
		return reagents;
	}
	
	/**
	 * Gets the power level of the spell. Spells start at a power level of 1.0.
	 * @return the power level
	 */
	public float getPower() {
		return power;
	}
	
	/**
	 * Gets the arguments passed to the spell if the spell was cast by command.
	 * @return the args, or null if there were none
	 */
	public String[] getSpellArgs() {
		return args;
	}
	
	/**
	 * Gets the post cast action that was executed for the spell cast.
	 * @return
	 */
	public PostCastAction getPostCastAction() {
		return action;
	}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
