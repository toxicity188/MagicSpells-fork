package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class DummySpell extends BuffSpell {

	private Set<UUID> entities;
	
	public DummySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashSet<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, Power power, String[] args) {
		entities.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}
	
	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		for (EffectPosition pos: EffectPosition.values()) {
			cancelEffectForAllPlayers(pos);
		}
		entities.clear();
	}

}
