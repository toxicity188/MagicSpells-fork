package com.nisovin.magicspells.events;

import java.util.Arrays;

import com.nisovin.magicspells.power.Power;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;

public class SpellPreImpactEvent extends SpellEvent implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();

	private LivingEntity target;
	private Power power;
	private Spell deliverySpell;
	private boolean redirect;
	private boolean cancelled;

	public SpellPreImpactEvent(Spell spellPayload, Spell deliverySpell, LivingEntity caster, LivingEntity target, Power power) {
		super(spellPayload, caster);
		this.target = target;
		this.power = power;
		this.deliverySpell = deliverySpell;
		redirect = false;
		cancelled = false;
		if (DebugHandler.isSpellPreImpactEventCheckEnabled()) MagicSpells.plugin.getLogger().info(toString());
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public LivingEntity getTarget() {
		return target;
	}
	
	public boolean getRedirected() {
		return redirect;
	}
	
	public void setRedirected(boolean redirect) {
		this.redirect = redirect;
	}
	
	public Power getPower() {
		return power;
	}
	
	public void setPower(Power power) {
		this.power = power;
	}
	
	public Spell getDeliverySpell() {
		return deliverySpell;
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
	public String toString() {
		String casterLabel = "Caster: " + (caster == null ? "null" : caster.toString());
		String targetLabel = "Target: " + (target == null ? "null" : target.toString());
		String spellLabel = "SpellPayload: " + (spell == null ? "null" : spell.toString());
		String payloadSpellLabel = "Delivery Spell: " + (deliverySpell == null ? "null" : deliverySpell.toString());
		return Arrays.deepToString(new String[]{ casterLabel, targetLabel, spellLabel, payloadSpellLabel });
	}
	
}
