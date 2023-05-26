package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

// TODO setup a system for registering "CleanseProvider"s
public class CleanseSpell extends TargetedSpell implements TargetedEntitySpell {

	private ValidTargetChecker checker;

	private List<String> toCleanse;
	private List<DotSpell> dotSpells;
	private List<StunSpell> stunSpells;
	private List<BuffSpell> buffSpells;
	private List<SilenceSpell> silenceSpells;
	private List<LevitateSpell> levitateSpells;
	private List<PotionEffectType> potionEffectTypes;

	private boolean fire;
	
	public CleanseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		toCleanse = getConfigStringList("remove", Arrays.asList("fire", "17", "19", "20"));
		dotSpells = new ArrayList<>();
		stunSpells = new ArrayList<>();
		buffSpells = new ArrayList<>();
		silenceSpells = new ArrayList<>();
		levitateSpells = new ArrayList<>();
		potionEffectTypes = new ArrayList<>();
		fire = false;
	}

	@Override
	public void initialize() {
		super.initialize();

		for (String s : toCleanse) {
			if (s.equalsIgnoreCase("fire")) {
				fire = true;
				continue;
			}

			if (s.startsWith("buff:")) {
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("buff:", ""));
				if (spell instanceof BuffSpell) buffSpells.add((BuffSpell) spell);
				continue;
			}

			if (s.startsWith("dot:")) {
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("dot:", ""));
				if (spell instanceof DotSpell) dotSpells.add((DotSpell) spell);
				continue;
			}

			if (s.startsWith("stun:")) {
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("stun:", ""));
				if (spell instanceof StunSpell) stunSpells.add((StunSpell) spell);
				continue;
			}

			if (s.startsWith("silence:")) {
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("silence:", ""));
				if (spell instanceof SilenceSpell) silenceSpells.add((SilenceSpell) spell);
				continue;
			}

			if (s.startsWith("levitate:")) {
				Spell spell = MagicSpells.getSpellByInternalName(s.replace("levitate:", ""));
				if (spell instanceof LevitateSpell) levitateSpells.add((LevitateSpell) spell);
				continue;
			}

			PotionEffectType type = Util.getPotionEffectType(s);
			if (type != null) potionEffectTypes.add(type);
		}

		checker = entity -> {
			if (fire && entity.getFireTicks() > 0) return true;

			for (PotionEffectType type : potionEffectTypes) {
				if (entity.hasPotionEffect(type)) return true;
			}

			for (BuffSpell spell : buffSpells) {
				if (spell.isActive(entity)) return true;
			}

			for (DotSpell spell : dotSpells) {
				if (spell.isActive(entity)) return true;
			}

			for (StunSpell spell : stunSpells) {
				if (spell.isStunned(entity)) return true;
			}

			for (SilenceSpell spell : silenceSpells) {
				if (spell.isSilenced(entity)) return true;
			}

			for (LevitateSpell spell : levitateSpells) {
				if (spell.isBeingLevitated(entity)) return true;
			}

			return false;
		};
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power, checker);
			if (target == null) return noTarget(livingEntity);
			
			cleanse(livingEntity, target.getTarget());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		cleanse(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		if (!validTargetList.canTarget(target)) return false;
		cleanse(null, target);
		return true;
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return checker;
	}
	
	private void cleanse(LivingEntity caster, LivingEntity target) {
		if (fire) target.setFireTicks(0);

		for (PotionEffectType type : potionEffectTypes) {
			target.addPotionEffect(new PotionEffect(type, 0, 0, true), true);
			target.removePotionEffect(type);
		}

		for (BuffSpell spell : buffSpells) {
			spell.turnOff(target);
		}

		for (DotSpell spell : dotSpells) {
			spell.cancelDot(target);
		}

		for (StunSpell spell : stunSpells) {
			spell.removeStun(target);
		}

		for (SilenceSpell spell : silenceSpells) {
			spell.removeSilence(target);
		}

		for (LevitateSpell spell : levitateSpells) {
			spell.removeLevitate(target);
		}

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);
	}

}
