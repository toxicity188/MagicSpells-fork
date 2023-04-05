package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import com.nisovin.magicspells.events.SpellApplyDotEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class DotSpell extends TargetedSpell implements TargetedEntitySpell, SpellDamageSpell {

	private Map<UUID, Dot> activeDots;

	private int delay;
	private int interval;
	private int duration;

	private float damage;

	private boolean preventKnockback;

	private String spellDamageType;

	public DotSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		delay = getConfigInt("delay", 1);
		interval = getConfigInt("interval", 20);
		duration = getConfigInt("duration", 200);

		damage = getConfigFloat("damage", 2);

		preventKnockback = getConfigBoolean("prevent-knockback", false);

		spellDamageType = getConfigString("spell-damage-type", "");

		activeDots = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			applyDot(livingEntity, targetInfo.getTarget(), targetInfo.getPower());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		applyDot(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		applyDot(null, target, power);
		return true;
	}

	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}

	public boolean isActive(LivingEntity entity) {
		return activeDots.containsKey(entity.getUniqueId());
	}

	public void cancelDot(LivingEntity entity) {
		if (!isActive(entity)) return;
		Dot dot = activeDots.get(entity.getUniqueId());
		dot.cancel();
	}
	
	private void applyDot(LivingEntity caster, LivingEntity target, float power) {
		Dot dot = activeDots.get(target.getUniqueId());
		if (dot != null) {
			dot.dur = 0;
			dot.power = power;
		} else {
			dot = new Dot(caster, target, power);
			activeDots.put(target.getUniqueId(), dot);
		}

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);
	}
	
	@EventHandler
	private void onDeath(PlayerDeathEvent event) {
		Dot dot = activeDots.get(event.getEntity().getUniqueId());
		if (dot != null) dot.cancel();
	}
	
	private class Dot implements Runnable {
		
		private LivingEntity caster;
		private LivingEntity target;
		private float power;

		private int taskId;
		private int dur = 0;


		private int limit;
		private Dot(LivingEntity caster, LivingEntity target, float power) {
			SpellApplyDotEvent event = new SpellApplyDotEvent(DotSpell.this,caster,target,damage,delay,interval,duration);
			EventUtil.call(event);
			this.caster = caster;
			this.target = target;
			this.power = power;
			limit = event.getDuration();
			taskId = MagicSpells.scheduleRepeatingTask(this, event.getDelay(), event.getInterval());
		}
		
		@Override
		public void run() {
			dur += interval;
			if (dur > limit) {
				cancel();
				return;
			}

			if (target.isDead() || !target.isValid()) {
				cancel();
				return;
			}

			double dam = damage * power;
			SpellApplyDamageEvent event = new SpellApplyDamageEvent(DotSpell.this, caster, target, dam, DamageCause.MAGIC, spellDamageType);
			EventUtil.call(event);
			dam = event.getFinalDamage();

			if (preventKnockback) {
				MagicSpellsEntityDamageByEntityEvent devent = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, damage);
				EventUtil.call(devent);
				if (!devent.isCancelled()) target.damage(devent.getDamage());
			} else target.damage(dam, caster);

			target.setNoDamageTicks(0);
			playSpellEffects(EffectPosition.DELAYED, target);
		}

		private void cancel() {
			MagicSpells.cancelTask(taskId);
			activeDots.remove(target.getUniqueId());
		}

	}

}
