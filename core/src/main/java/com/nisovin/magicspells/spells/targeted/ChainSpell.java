package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.SingleSpellSupplier;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class ChainSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell, SingleSpellSupplier {

	private int bounces;
	private int interval;

	private double bounceRange;

	private String spellToCastName;
	private Subspell spellToCast;

	@Override
	public Spell getSubSpell() {
		return spellToCast != null ? spellToCast.getSpell() : null;
	}

	public ChainSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		bounces = getConfigInt("bounces", 3);
		interval = getConfigInt("interval", 10);

		bounceRange = getConfigDouble("bounce-range", 8);

		spellToCastName = getConfigString("spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process()) {
			spellToCast = null;
			MagicSpells.error("ChainSpell '" + internalName + "' has an invalid spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			chain(livingEntity, livingEntity.getLocation(), target.getTarget(), target.getPower());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		chain(caster, caster.getLocation(), target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		chain(null, null, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		chain(caster, from, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		chain(null, from, target, power);
		return true;
	}

	private void chain(LivingEntity livingEntity, Location start, LivingEntity target, float power) {
		List<LivingEntity> targets = new ArrayList<>();
		List<Float> targetPowers = new ArrayList<>();
		targets.add(target);
		targetPowers.add(power);

		// Get targets
		LivingEntity current = target;
		int attempts = 0;
		while (targets.size() < bounces && attempts++ < bounces << 1) {
			List<Entity> entities = current.getNearbyEntities(bounceRange, bounceRange, bounceRange);
			for (Entity e : entities) {
				if (!(e instanceof LivingEntity)) continue;
				if (targets.contains(e)) continue;

				if (!validTargetList.canTarget(livingEntity, target)) continue;

				float thisPower = power;
				if (livingEntity != null) {
					SpellTargetEvent event = new SpellTargetEvent(this, livingEntity, (LivingEntity) e, thisPower);
					EventUtil.call(event);
					if (event.isCancelled()) continue;
					thisPower = event.getPower();
				}

				targets.add((LivingEntity) e);
				targetPowers.add(thisPower);
				current = (LivingEntity) e;
				break;
			}
		}

		// Cast spell at targets
		if (livingEntity != null) playSpellEffects(EffectPosition.CASTER, livingEntity);
		else if (start != null) playSpellEffects(EffectPosition.CASTER, start);

		if (interval <= 0) {
			for (int i = 0; i < targets.size(); i++) {
				Location from;
				if (i == 0) from = start;
				else from = targets.get(i - 1).getLocation();

				castSpellAt(livingEntity, from, targets.get(i), targetPowers.get(i));
				if (i > 0) playSpellEffectsTrail(targets.get(i - 1).getLocation(), targets.get(i).getLocation());
				else if (i == 0 && livingEntity != null) playSpellEffectsTrail(livingEntity.getLocation(), targets.get(i).getLocation());
				playSpellEffects(EffectPosition.TARGET, targets.get(i));
			}
		} else new ChainBouncer(livingEntity, start, targets, power);
	}

	private boolean castSpellAt(LivingEntity caster, Location from, LivingEntity target, float power) {
		if (spellToCast.isTargetedEntityFromLocationSpell() && from != null) return spellToCast.castAtEntityFromLocation(caster, from, target, power);
		if (spellToCast.isTargetedEntitySpell()) return spellToCast.castAtEntity(caster, target, power);
		if (spellToCast.isTargetedLocationSpell()) return spellToCast.castAtLocation(caster, target.getLocation(), power);
		return true;
	}

	private class ChainBouncer implements Runnable {

		private LivingEntity caster;
		private Location start;
		private List<LivingEntity> targets;
		private float power;
		private int current = 0;
		private int taskId;

		private ChainBouncer(LivingEntity caster, Location start, List<LivingEntity> targets, float power) {
			this.caster = caster;
			this.start = start;
			this.targets = targets;
			this.power = power;
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, interval);
		}

		@Override
		public void run() {
			Location from;
			if (current == 0) from = start;
			else from = targets.get(current - 1).getLocation();

			castSpellAt(caster, from, targets.get(current), power);
			if (current > 0) {
				playSpellEffectsTrail(targets.get(current - 1).getLocation().add(0, 0.5, 0), targets.get(current).getLocation().add(0, 0.5, 0));
			} else if (current == 0 && caster != null) {
				playSpellEffectsTrail(caster.getLocation().add(0, 0.5, 0), targets.get(current).getLocation().add(0, 0.5, 0));
			}

			playSpellEffects(EffectPosition.TARGET, targets.get(current));
			current++;
			if (current >= targets.size()) MagicSpells.cancelTask(taskId);
		}

	}

}
