package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.events.SpellCastEvent;

public class ClaritySpell extends BuffSpell {

	private Map<UUID, Power> buffed;

	private float multiplier;
	private SpellFilter filter;

	public ClaritySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		multiplier = getConfigFloat("multiplier", 0.5F);

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);
		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);

		buffed = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, Power power, String[] args) {
		buffed.put(entity.getUniqueId(), power);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return buffed.containsKey(entity.getUniqueId());
	}

	@Override
	protected void turnOffBuff(LivingEntity entity) {
		buffed.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		buffed.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellCast(SpellCastEvent event) {
		LivingEntity caster = event.getCaster();
		if (!isActive(caster)) return;
		if (!filter.check(event.getSpell())) return;

		float mod = multiplier;
		Power power = buffed.get(caster.getUniqueId());

		if (multiplier < 1) mod *= 1 / power.floatValue();
		else if (multiplier > 1) mod *= power.floatValue();

		SpellReagents reagents = event.getReagents();
		if (reagents != null) event.setReagents(reagents.multiply(mod));
		
		addUseAndChargeCost(caster);
	}

}
