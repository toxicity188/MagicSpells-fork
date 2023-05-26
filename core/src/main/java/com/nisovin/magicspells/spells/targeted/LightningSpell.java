package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class LightningSpell extends TargetedSpell implements TargetedLocationSpell {

	private double additionalDamage;

	private boolean zapPigs;
	private boolean noDamage;
	private boolean checkPlugins;
	private boolean chargeCreepers;
	private boolean requireEntityTarget;
	
	public LightningSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		additionalDamage = getConfigFloat("additional-damage", 0F);

		zapPigs = getConfigBoolean("zap-pigs", true);
		noDamage = getConfigBoolean("no-damage", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		chargeCreepers = getConfigBoolean("charge-creepers", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target;
			LivingEntity entityTarget = null;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
				if (targetInfo != null) {
					entityTarget = targetInfo.getTarget();
					power = targetInfo.getPower();
				}
				if (checkPlugins) {
					MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(livingEntity, entityTarget, DamageCause.ENTITY_ATTACK, 1 + additionalDamage);
					EventUtil.call(event);
					if (event.isCancelled()) entityTarget = null;
				}
				if (entityTarget != null) {
					target = entityTarget.getLocation().getBlock();
					if (additionalDamage > 0) entityTarget.damage(additionalDamage * power.doubleValue(), livingEntity);
				} else return noTarget(livingEntity);
			} else {
				try {
					target = getTargetedBlock(livingEntity, power);
				} catch (IllegalStateException e) {
					DebugHandler.debugIllegalState(e);
					target = null;
				}
				if (target != null) {
					SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, livingEntity, target.getLocation(), power);
					EventUtil.call(event);
					if (event.isCancelled()) {
						target = null;
					} else target = event.getTargetLocation().getBlock();
				}
			}
			if (target != null) {
				lightning(target.getLocation());
				playSpellEffects(livingEntity, target.getLocation());
				if (entityTarget != null) {
					sendMessages(livingEntity, entityTarget);
					return PostCastAction.NO_MESSAGES;
				}
			} else return noTarget(livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		lightning(target);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		lightning(target);
		playSpellEffects(EffectPosition.CASTER, target);
		return true;
	}
	
	private void lightning(Location target) {
		if (noDamage) target.getWorld().strikeLightningEffect(target);
		else {
			LightningStrike strike = target.getWorld().strikeLightning(target);
			strike.setMetadata("MS" + internalName, new FixedMetadataValue(MagicSpells.plugin, new ChargeOption(chargeCreepers, zapPigs)));
		}
	}
	
	@EventHandler
	public void onCreeperCharge(CreeperPowerEvent event) {
		LightningStrike strike = event.getLightning();
		if (strike == null) return;
		List<MetadataValue> data = strike.getMetadata("MS" + internalName);
		if (data == null || data.isEmpty()) return;
		for (MetadataValue val: data) {
			ChargeOption option = (ChargeOption)val.value();
			if (!option.chargeCreeper) event.setCancelled(true);
			break;
		}
	}
	
	@EventHandler
	public void onPigZap(PigZapEvent event) {
		LightningStrike strike = event.getLightning();
		if (strike == null) return;
		List<MetadataValue> data = strike.getMetadata("MS" + internalName);
		if (data == null || data.isEmpty()) return;
		for (MetadataValue val: data) {
			ChargeOption option = (ChargeOption) val.value();
			if (!option.changePig) event.setCancelled(true);
		}
	}
	
	private static class ChargeOption {

		private boolean changePig;
		private boolean chargeCreeper;

		private ChargeOption(boolean creeper, boolean pigmen) {
			changePig = pigmen;
			chargeCreeper = creeper;
		}
		
	}
	
}
