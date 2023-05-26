package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import com.nisovin.magicspells.power.Power;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class PainSpell extends TargetedSpell implements TargetedEntitySpell, SpellDamageSpell {

	private String spellDamageType;
	private DamageCause damageType;

	private double damage;

	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean avoidDamageModification;
	private boolean tryAvoidingAntiCheatPlugins;
	
	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellDamageType = getConfigString("spell-damage-type", "");
		String type = getConfigString("damage-type", "ENTITY_ATTACK");
		for (DamageCause cause : DamageCause.values()) {
			if (cause.name().equalsIgnoreCase(type)) {
				damageType = cause;
				break;
			}
		}
		if (damageType == null) {
			DebugHandler.debugBadEnumValue(DamageCause.class, type);
			damageType = DamageCause.ENTITY_ATTACK;
		}

		damage = getConfigFloat("damage", 4);

		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);
		tryAvoidingAntiCheatPlugins = getConfigBoolean("try-avoiding-anticheat-plugins", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			boolean done;
			if (livingEntity instanceof Player) done = CompatBasics.exemptAction(() -> causePain(livingEntity, target.getTarget(), target.getPower()), (Player) livingEntity, CompatBasics.activeExemptionAssistant.getPainExemptions());
			else done = causePain(livingEntity, target.getTarget(), target.getPower());
			if (!done) return noTarget(livingEntity);
			
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return causePain(caster, target, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		if (!validTargetList.canTarget(target)) return false;
		return causePain(null, target, power);
	}

	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}
	
	private boolean causePain(LivingEntity caster, LivingEntity target, Power power) {
		if (target == null) return false;
		if (target.isDead()) return false;
		double localDamage = damage * power.doubleValue();

		if (checkPlugins) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, damageType, localDamage);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			if (!avoidDamageModification) localDamage = event.getDamage();
			target.setLastDamageCause(event);
		}

		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, caster, target, localDamage, damageType, spellDamageType);
		EventUtil.call(event);
		localDamage = event.getFinalDamage();

		if (ignoreArmor) {
			double health = target.getHealth();
			if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
			health = health - localDamage;
			if (health < 0) health = 0;
			if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
			if (health == 0 && caster instanceof Player) MagicSpells.getVolatileCodeHandler().setKiller(target, (Player) caster);

			target.setHealth(health);
			playSpellEffects(caster, target);
			target.playEffect(EntityEffect.HURT);
			return true;
		}

		if (tryAvoidingAntiCheatPlugins) target.damage(localDamage);
		else target.damage(localDamage, caster);
		playSpellEffects(caster, target);
		return true;
	}

}
