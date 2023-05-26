package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CrippleSpell extends TargetedSpell implements TargetedEntitySpell {

	private int strength;
	private int duration;
	private int portalCooldown;

	private boolean useSlownessEffect;
	private boolean applyPortalCooldown;

	public CrippleSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strength = getConfigInt("effect-strength", 5);
		duration = getConfigInt("effect-duration", 100);
		portalCooldown = getConfigInt("portal-cooldown-ticks", 100);

		useSlownessEffect = getConfigBoolean("use-slowness-effect", true);
		applyPortalCooldown = getConfigBoolean("apply-portal-cooldown", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			cripple(livingEntity, target.getTarget(), power);
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		cripple(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		if (!validTargetList.canTarget(target)) return false;
		cripple(null, target, power);
		return true;
	}
	
	private void cripple(LivingEntity caster, LivingEntity target, Power power) {
		if (target == null) return;

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);
		
		if (useSlownessEffect) target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Math.round(duration * power.intValue()), strength), true);
		if (applyPortalCooldown && target.getPortalCooldown() < (portalCooldown * power.intValue())) target.setPortalCooldown((portalCooldown * power.intValue()));
	}

}
