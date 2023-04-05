package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.events.SpellCastEvent;

public class EmpowerSpell extends BuffSpell {

	private Map<UUID, Float> empowered;

	private float maxPower;
	private float extraPower;

	private SpellFilter filter;

	public EmpowerSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxPower = getConfigFloat("max-power-multiplier", 1.5F);
		extraPower = getConfigFloat("power-multiplier", 1.5F);
		
		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);
		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);

		empowered = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		float p = power * extraPower;
		if (p > maxPower) p = maxPower;
		empowered.put(entity.getUniqueId(), p);
		return true;
	}

	@Override
	public boolean recastBuff(LivingEntity entity, float power, String[] args) {
		return castBuff(entity, power, args);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return empowered.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		empowered.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		empowered.clear();
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onSpellCast(SpellCastEvent event) {
		LivingEntity player = event.getCaster();
		if (player == null) return;
		if (!isActive(player)) return;
		if (!filter.check(event.getSpell())) return;

		addUseAndChargeCost(player);
		event.increasePower(empowered.get(player.getUniqueId()));
	}

}
