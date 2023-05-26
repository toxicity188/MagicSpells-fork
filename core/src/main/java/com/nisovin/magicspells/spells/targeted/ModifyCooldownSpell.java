package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ModifyCooldownSpell extends TargetedSpell implements TargetedEntitySpell {

	private List<Spell> spells;
	private List<String> spellNames;
	
	private float seconds;
	private float multiplier;
	
	public ModifyCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		spellNames = getConfigStringList("spells", null);

		seconds = getConfigFloat("seconds", 1F);
		multiplier = getConfigFloat("multiplier", 0F);
	}
	
	@Override
	public void initialize() {
		spells = new ArrayList<>();

		if (spellNames == null) {
			MagicSpells.error("ModifyCooldownSpell '" + internalName + "' has no spells defined!");
			return;
		}

		for (String spellName : spellNames) {
			Spell spell = MagicSpells.getSpellByInternalName(spellName);
			if (spell == null) {
				MagicSpells.error("ModifyCooldownSpell '" + internalName + "' has an invalid spell defined '" + spellName + '\'');
				continue;
			}
			spells.add(spell);
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			modifyCooldowns(target.getTarget(), target.getPower());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		modifyCooldowns(target, power);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		modifyCooldowns(target, power);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}

	private void modifyCooldowns(LivingEntity target, Power power) {
		float sec = seconds * power.floatValue();
		float mult = multiplier * (1F / power.floatValue());

		for (Spell spell : spells) {
			float cd = spell.getCooldown(target);
			if (cd <= 0) continue;

			cd -= sec;
			if (mult > 0) cd *= mult;
			if (cd < 0) cd = 0;
			spell.setCooldown(target, cd, false);
		}
	}

}
