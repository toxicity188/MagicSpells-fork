package com.nisovin.magicspells.events;

import com.nisovin.magicspells.power.Power;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.ProjectileTracker;

public class ParticleProjectileHitEvent extends SpellEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private LivingEntity target;
	private ProjectileTracker tracker;
	private Power power;
	private boolean cancelled = false;

	public ParticleProjectileHitEvent(LivingEntity caster, LivingEntity target, ProjectileTracker tracker, Spell spell, Power power) {
		super(spell, caster);

		this.target = target;
		this.tracker = tracker;
		this.power = power;
	}

	public ProjectileTracker getTracker() {
		return tracker;
	}

	public void setTracker(ProjectileTracker tracker) {
		this.tracker = tracker;
	}

	public LivingEntity getTarget() {
		return target;
	}

	public void setTarget(LivingEntity target) {
		this.target = target;
	}

	public Power getPower() {
		return power;
	}

	public void setPower(Power power) {
		this.power = power;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
