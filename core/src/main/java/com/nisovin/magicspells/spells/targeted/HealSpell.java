package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.events.SpellApplyHealEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityRegainHealthEvent;

public class HealSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private final double healAmount;
	private final double healAmountPercentage;

	private final boolean checkPlugins;
	private final boolean cancelIfFull;

	private final String strMaxHealth;

	private final ValidTargetChecker checker;

	public HealSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		healAmount = getConfigFloat("heal-amount", 10);
		healAmountPercentage = getConfigFloat("heal-amount-percentage", 0);

		checkPlugins = getConfigBoolean("check-plugins", true);
		cancelIfFull = getConfigBoolean("cancel-if-full", true);

		strMaxHealth = getConfigString("str-max-health", "%t is already at max health.");

		checker = (LivingEntity entity) -> entity.getHealth() < Util.getMaxHealth(entity);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power, checker);
			if (targetInfo == null) return noTarget(livingEntity);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			if (cancelIfFull && target.getHealth() == Util.getMaxHealth(target)) return noTarget(livingEntity, formatMessage(strMaxHealth, "%t", getTargetName(target)));
			boolean healed = heal(livingEntity, target, power);
			if (!healed) return noTarget(livingEntity);
			sendMessages(livingEntity, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (validTargetList.canTarget(caster, target) && target.getHealth() < Util.getMaxHealth(target)) return heal(caster, target, power);
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (validTargetList.canTarget(target) && target.getHealth() < Util.getMaxHealth(target)) return heal(null, target, power);
		return false;
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return checker;
	}
	
	private boolean heal(LivingEntity livingEntity, LivingEntity target, float power) {
		double health = target.getHealth();
		double maxHealth = Util.getMaxHealth(target);

		SpellApplyHealEvent event = new SpellApplyHealEvent(this,livingEntity,target,healAmount * power + healAmountPercentage/100*maxHealth);
		EventUtil.call(event);
		double amt = event.getAmount();

		if (checkPlugins) {
			MagicSpellsEntityRegainHealthEvent evt = new MagicSpellsEntityRegainHealthEvent(target, amt, RegainReason.CUSTOM);
			EventUtil.call(evt);
			if (evt.isCancelled()) return false;
			amt = evt.getAmount();
		}

		health += amt;
		if (health > maxHealth) health = maxHealth;
		target.setHealth(health);

		if (livingEntity == null) playSpellEffects(EffectPosition.TARGET, target);
		else playSpellEffects(livingEntity, target);
		return true;
	}

}
