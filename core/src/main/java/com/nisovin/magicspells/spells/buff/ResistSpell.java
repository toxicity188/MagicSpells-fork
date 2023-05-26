package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class ResistSpell extends BuffSpell {

	private Map<UUID, Power> buffed;

	private float multiplier;

	private List<String> spellDamageTypes;
	private List<DamageCause> normalDamageTypes;

	public ResistSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		multiplier = getConfigFloat("multiplier", 0.5F);

		spellDamageTypes = getConfigStringList("spell-damage-types", null);
		List<String> list = getConfigStringList("normal-damage-types", null);

		if (list != null) {
			normalDamageTypes = new ArrayList<>();
			for (String s : list) {
				for (DamageCause cause : DamageCause.values()) {
					if (!cause.name().equalsIgnoreCase(s)) continue;
					normalDamageTypes.add(cause);
					break;
				}
			}
			if (normalDamageTypes.isEmpty()) normalDamageTypes = null;
		}

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
	
	@EventHandler
	public void onSpellDamage(SpellApplyDamageEvent event) {
		if (spellDamageTypes == null) return;
		if (!(event.getSpell() instanceof SpellDamageSpell)) return;
		if (!isActive(event.getTarget())) return;

		SpellDamageSpell spell = (SpellDamageSpell) event.getSpell();
		String spellDamageType = spell.getSpellDamageType();
		if (spellDamageType == null) return;
		if (!spellDamageTypes.contains(spellDamageType)) return;

		LivingEntity entity = event.getTarget();

		Power power = new Power(multiplier);
		if (multiplier < 1) power.multiply(new Power(1 / buffed.get(entity.getUniqueId()).floatValue()));
		else if (multiplier > 1) power.multiply(buffed.get(entity.getUniqueId()));

		addUseAndChargeCost(entity);
		event.applyDamageModifier(power.floatValue());
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (normalDamageTypes == null) return;
		if (!normalDamageTypes.contains(event.getCause())) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity)) return;
		if (!isActive((LivingEntity) entity)) return;

		float mult = multiplier;
		if (multiplier < 1) mult *= 1 / buffed.get(entity.getUniqueId()).floatValue();
		else if (multiplier > 1) mult *= buffed.get(entity.getUniqueId()).floatValue();

		addUseAndChargeCost((LivingEntity) entity);
		event.setDamage(event.getDamage() * mult);
	}

}
