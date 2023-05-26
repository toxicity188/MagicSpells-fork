package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ConfusionSpell extends InstantSpell implements TargetedLocationSpell {

	private double radius;
	
	public ConfusionSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigDouble("radius", 10);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			confuse(livingEntity, livingEntity.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		confuse(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		return false;
	}

	private void confuse(LivingEntity caster, Location location, Power power) {
		double castingRange = Math.round(radius * power.doubleValue());
		Collection<Entity> entities = location.getWorld().getNearbyEntities(location, castingRange, castingRange, castingRange);
		List<LivingEntity> monsters = new ArrayList<>();

		for (Entity e : entities) {
			if (!(e instanceof LivingEntity)) continue;
			if (!validTargetList.canTarget(caster, e)) continue;
			monsters.add((LivingEntity) e);
		}

		for (int i = 0; i < monsters.size(); i++) {
			int next = i + 1;
			if (next >= monsters.size()) next = 0;
			MagicSpells.getVolatileCodeHandler().setTarget(monsters.get(i), monsters.get(next));
			playSpellEffects(EffectPosition.TARGET, monsters.get(i));
			playSpellEffectsTrail(caster.getLocation(), monsters.get(i).getLocation());
		}
		playSpellEffects(EffectPosition.CASTER, caster);
	}

}
