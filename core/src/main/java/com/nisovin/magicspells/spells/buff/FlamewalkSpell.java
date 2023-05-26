package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class FlamewalkSpell extends BuffSpell {

	private Map<UUID, Power> flamewalkers;

	private int radius;
	private int fireTicks;
	private int tickInterval;
	private boolean checkPlugins;

	private Burner burner;
	
	public FlamewalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigInt("radius", 8);
		fireTicks = getConfigInt("fire-ticks", 80);
		tickInterval = getConfigInt("tick-interval", 100);
		checkPlugins = getConfigBoolean("check-plugins", true);

		flamewalkers = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, Power power, String[] args) {
		flamewalkers.put(entity.getUniqueId(), power);
		if (burner == null) burner = new Burner();
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return flamewalkers.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		flamewalkers.remove(entity.getUniqueId());
		if (!flamewalkers.isEmpty()) return;
		if (burner == null) return;
		
		burner.stop();
		burner = null;
	}
	
	@Override
	protected void turnOff() {
		flamewalkers.clear();
		if (burner == null) return;
		
		burner.stop();
		burner = null;
	}

	private class Burner implements Runnable {
		
		private int taskId;

		private Burner() {
			taskId = MagicSpells.scheduleRepeatingTask(this, tickInterval, tickInterval);
		}
		
		public void stop() {
			MagicSpells.cancelTask(taskId);
		}
		
		@Override
		public void run() {
			for (UUID id : flamewalkers.keySet()) {
				Entity entity = Bukkit.getEntity(id);
				if (!(entity instanceof LivingEntity)) continue;
				LivingEntity livingEntity = (LivingEntity) entity;

				if (isExpired(livingEntity)) {
					turnOff(livingEntity);
					continue;
				}

				Power power = flamewalkers.get(livingEntity.getUniqueId());
				playSpellEffects(EffectPosition.DELAYED, livingEntity);

				List<Entity> entities = livingEntity.getNearbyEntities(radius, radius, radius);
				for (Entity target : entities) {
					if (!(target instanceof LivingEntity)) continue;
					if (validTargetList != null && !validTargetList.canTarget(target)) continue;
					if (livingEntity.equals(target)) continue;
					if (checkPlugins) {
						MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(livingEntity, entity, DamageCause.ENTITY_ATTACK, 1);
						EventUtil.call(event);
						if (event.isCancelled()) continue;
					}

					target.setFireTicks(Math.round(fireTicks * power.intValue()));
					addUseAndChargeCost(livingEntity);
					playSpellEffects(EffectPosition.TARGET, target);
					playSpellEffectsTrail(livingEntity.getLocation(), target.getLocation());
				}

			}

		}
		
	}

}
