package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellPreImpactEvent;

public class ReflectSpell extends BuffSpell {

	private Map<UUID, Power> reflectors;
	private Set<String> shieldBreakerNames;
	private Set<String> delayedReflectionSpells;

	private float reflectedSpellPowerMultiplier;

	private boolean spellPowerAffectsReflectedPower;
	private boolean delayedReflectionSpellsUsePayloadShieldBreaker;

	public ReflectSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		reflectors = new HashMap<>();
		shieldBreakerNames = new HashSet<>();
		delayedReflectionSpells = new HashSet<>();

		shieldBreakerNames.addAll(getConfigStringList("shield-breakers", new ArrayList<>()));
		delayedReflectionSpells.addAll(getConfigStringList("delayed-reflection-spells", new ArrayList<>()));

		reflectedSpellPowerMultiplier = (float) getConfigDouble("reflected-spell-power-multiplier", 1F);

		spellPowerAffectsReflectedPower = getConfigBoolean("spell-power-affects-reflected-power", false);
		delayedReflectionSpellsUsePayloadShieldBreaker = getConfigBoolean("delayed-reflection-spells-use-payload-shield-breaker", true);
	}

	@Override
	public boolean castBuff(LivingEntity entity, Power power, String[] args) {
		reflectors.put(entity.getUniqueId(), power);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return reflectors.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		reflectors.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		reflectors.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent event) {
		LivingEntity target = event.getTarget();
		if (target == null) return;
		if (!target.isValid()) return;
		if (!isActive(target)) return;

		Power power = reflectors.get(target.getUniqueId());
		if (shieldBreakerNames != null && shieldBreakerNames.contains(event.getSpell().getInternalName())) {
			turnOff(target);
			return;
		}
		if (delayedReflectionSpells != null && delayedReflectionSpells.contains(event.getSpell().getInternalName())) {
			// Let the delayed reflection spells target the reflector so the animations run
			// It will get reflected later
			return;
		}

		if (!chargeUseCost(target)) return;

		addUse(target);
		event.setTarget(event.getCaster());
		event.setPower(event.getPower().multiply(new Power(reflectedSpellPowerMultiplier * (spellPowerAffectsReflectedPower ? power.floatValue() : 1))));
	}

	@EventHandler
	public void onSpellPreImpact(SpellPreImpactEvent event) {
		LivingEntity target = event.getTarget();

		if (event == null) {
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("SpellPreImpactEvent was null!");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return;
		}
		if (target == null) {
			MagicSpells.plugin.getLogger().warning("Spell preimpact event had a null target, the spell cannot be reflected.");
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("Spell preimpact event had a null target");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return;
		}
		if (event.getCaster() == null) {
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("SpellPreImpactEvent had a null caster!");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return;
		}

		if (!isActive(target)) return;
		if (delayedReflectionSpellsUsePayloadShieldBreaker && (event.getSpell() != null && shieldBreakerNames.contains(event.getSpell().getInternalName()))) {
			turnOff(target);
			return;
		}

		addUse(target);
		event.setRedirected(true);
		Power powerMultiplier = new Power(reflectedSpellPowerMultiplier * (spellPowerAffectsReflectedPower ? (reflectors.get(target.getUniqueId()) == null ? 1F: reflectors.get(target.getUniqueId())).floatValue() : 1F));
		event.setPower(event.getPower().multiply(powerMultiplier));

	}

}
