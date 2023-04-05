package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class WindglideSpell extends BuffSpell {

	private Set<UUID> gliders;

	private Subspell glideSpell;
	private Subspell collisionSpell;
	private String glideSpellName;
	private String collisionSpellName;

	private boolean cancelOnCollision;
	private boolean blockCollisionDmg;

	private int interval;
	private float height;
	private float velocity;

	private GlideMonitor monitor;

	public WindglideSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		glideSpellName = getConfigString("spell", "");
		collisionSpellName = getConfigString("collision-spell", "");

		blockCollisionDmg = getConfigBoolean("block-collision-dmg", true);
		cancelOnCollision = getConfigBoolean("cancel-on-collision", false);

		height = getConfigFloat("height", 0F);
		interval = getConfigInt("interval", 4);
		velocity = getConfigFloat("velocity", 20F) / 10;
		if (interval <= 0) interval = 4;

		gliders = new HashSet<>();
		monitor = new GlideMonitor();
	}

	@Override
	public void initialize() {
		super.initialize();

		glideSpell = new Subspell(glideSpellName);
		if (!glideSpell.process() || !glideSpell.isTargetedLocationSpell()) {
			glideSpell = null;
			if (!glideSpellName.isEmpty()) MagicSpells.error("WindglideSpell " + internalName + " has an invalid spell defined");
		}

		collisionSpell = new Subspell(collisionSpellName);
		if (!collisionSpell.process() || !collisionSpell.isTargetedLocationSpell()) {
			collisionSpell = null;
			if (!collisionSpellName.isEmpty()) MagicSpells.error("WindglideSpell " + internalName + " has an invalid collision-spell defined");
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		gliders.add(entity.getUniqueId());
		entity.setGliding(true);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return gliders.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		gliders.remove(entity.getUniqueId());
		entity.setGliding(false);
	}

	@Override
	protected void turnOff() {
		for (EffectPosition pos: EffectPosition.values()) {
			cancelEffectForAllPlayers(pos);
		}

		for (UUID id : gliders) {
			Entity entity = Bukkit.getEntity(id);
			if (entity == null) continue;
			if (!(entity instanceof LivingEntity)) continue;
			if (!entity.isValid()) continue;

			((LivingEntity) entity).setGliding(false);
			turnOffBuff((LivingEntity) entity);
		}

		gliders.clear();
	}

	@EventHandler
	public void onEntityGlide(EntityToggleGlideEvent e) {
		Entity entity = e.getEntity();
		if (!(entity instanceof LivingEntity)) return;
		LivingEntity livingEntity = (LivingEntity) entity;
		if (!isActive(livingEntity)) return;
		if (livingEntity.isGliding()) e.setCancelled(true);
	}

	@EventHandler
	public void onEntityCollision(EntityDamageEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) return;
		if (!(e.getEntity() instanceof LivingEntity)) return;
		LivingEntity livingEntity = (LivingEntity) e.getEntity();
		if (!isActive(livingEntity)) return;

		if (blockCollisionDmg) e.setCancelled(true);
		if (cancelOnCollision) turnOff(livingEntity);
		if (collisionSpell != null) collisionSpell.castAtLocation(livingEntity, livingEntity.getLocation(), 1F);
	}

	private class GlideMonitor implements Runnable {

		private int taskId;

		private GlideMonitor() {
			taskId = MagicSpells.scheduleRepeatingTask(this, interval, interval);
		}

		@Override
		public void run() {
			for (UUID id : gliders) {
				Entity entity = Bukkit.getEntity(id);
				if (entity == null || !entity.isValid()) continue;
				if (!(entity instanceof LivingEntity)) continue;

				Location eLoc = entity.getLocation();
				Vector v = eLoc.getDirection().normalize().multiply(velocity).add(new Vector(0, height, 0));
				entity.setVelocity(v);

				if (glideSpell != null) glideSpell.castAtLocation((LivingEntity) entity, eLoc, 1F);
				playSpellEffects(EffectPosition.SPECIAL, eLoc);
				addUseAndChargeCost((LivingEntity) entity);
			}
		}

		public void stop() {
			MagicSpells.cancelTask(taskId);
		}

	}

}
