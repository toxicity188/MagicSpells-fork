package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class StealthSpell extends BuffSpell {
	
	private Set<UUID> stealthy;
	
	public StealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		stealthy = new HashSet<>();
	}
	
	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		stealthy.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return stealthy.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		stealthy.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		stealthy.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		if (!(event.getTarget() instanceof LivingEntity)) return;
		LivingEntity target = (LivingEntity) event.getTarget();
		if (!isActive(target)) return;
		if (isExpired(target)) {
			turnOff(target);
			return;
		}

		addUseAndChargeCost(target);
		event.setCancelled(true);
	}
	
}
