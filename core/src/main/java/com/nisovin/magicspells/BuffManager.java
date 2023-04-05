package com.nisovin.magicspells;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.zones.NoMagicZoneManager;

public class BuffManager {

	private Map<LivingEntity, Set<BuffSpell>> activeBuffs;
	private Map<LivingEntity, Set<BuffSpell>> toRemove;

	private int interval;
	private Monitor monitor;

	public BuffManager(int interval) {
		this.interval = interval;
		activeBuffs = new HashMap<>();
		toRemove = new HashMap<>();
		monitor = new Monitor();
	}

	public void addBuff(LivingEntity entity, BuffSpell spell) {
		Set<BuffSpell> buffs = activeBuffs.computeIfAbsent(entity, s -> new HashSet<>());
		// Sanity Check
		if (buffs == null) throw new IllegalStateException("buffs should not be null here");
		buffs.add(spell);

		monitor.run();
	}

	public void removeBuff(LivingEntity entity, BuffSpell spell) {
		Set<BuffSpell> buffs = activeBuffs.get(entity);
		if (buffs == null) return;
		buffs.remove(spell);
		if (buffs.isEmpty()) activeBuffs.remove(entity);
	}

	public Map<LivingEntity, Set<BuffSpell>> getActiveBuffs() {
		return activeBuffs;
	}

	public Set<BuffSpell> getActiveBuffs(LivingEntity entity) {
		return activeBuffs.get(entity);
	}

	public void turnOff() {
		MagicSpells.cancelTask(monitor.taskId);
		monitor = null;
		activeBuffs.clear();
		toRemove.clear();
		toRemove = null;
		activeBuffs = null;
	}

	private class Monitor implements Runnable {

		private int taskId;

		private Monitor() {
			taskId = MagicSpells.scheduleRepeatingTask(this, interval, interval);
		}

		@Override
		public void run() {
			NoMagicZoneManager zoneManager = MagicSpells.getNoMagicZoneManager();
			if (zoneManager == null) return;

			for (LivingEntity entity : activeBuffs.keySet()) {
				if (entity == null) continue;
				if (entity instanceof Player) {
					Set<BuffSpell> buffs = new HashSet<>(activeBuffs.get(entity));
					Set<BuffSpell> removeBuffs = new HashSet<>();
					for (BuffSpell spell : buffs) {
						if (zoneManager.willFizzle(entity, spell)) {
							removeBuffs.add(spell);
						}
					}
					toRemove.put(entity, removeBuffs);
					continue;
				}

				if (entity.isValid()) continue;
				if (!entity.isDead()) continue;
				Set<BuffSpell> buffs = new HashSet<>(activeBuffs.get(entity));
				Set<BuffSpell> removeBuffs = new HashSet<>(buffs);
				toRemove.put(entity, removeBuffs);
			}

			for (LivingEntity entity : toRemove.keySet()) {
				Set<BuffSpell> removeBuffs = toRemove.get(entity);
				for (BuffSpell spell : removeBuffs) {
					spell.turnOff(entity);
				}
			}

			toRemove.clear();

		}

		public void stop() {
			MagicSpells.cancelTask(taskId);
		}

	}

}
