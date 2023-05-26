package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.power.Power;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public final class TargetedMultiSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static final Pattern DELAY_PATTERN = Pattern.compile("DELAY [0-9]+");

	private Random random;

	private List<Action> actions;
	private List<String> spellList;

	private float yOffset;

	private boolean pointBlank;
	private boolean stopOnFail;
	private boolean requireEntityTarget;
	private boolean castRandomSpellInstead;

	public TargetedMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		random = new Random();

		actions = new ArrayList<>();
		spellList = getConfigStringList("spells", null);

		yOffset = getConfigFloat("y-offset", 0F);

		pointBlank = getConfigBoolean("point-blank", false);
		stopOnFail = getConfigBoolean("stop-on-fail", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		castRandomSpellInstead = getConfigBoolean("cast-random-spell-instead", false);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (spellList != null) {
			for (String s : spellList) {
				if (RegexUtil.matches(DELAY_PATTERN, s)) {
					int delay = Integer.parseInt(s.split(" ")[1]);
					actions.add(new Action(delay));
				} else {
					Subspell spell = new Subspell(s);
					if (spell.process()) actions.add(new Action(spell));
					else MagicSpells.error("TargetedMultiSpell '" + internalName + "' has an invalid spell '" + s + "' defined!");
				}
			}
		}
		spellList = null;
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location locTarget = null;
			LivingEntity entTarget = null;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> info = getTargetedEntity(livingEntity, power);
				if (info != null) {
					entTarget = info.getTarget();
					power = info.getPower();
				}
			} else if (pointBlank) {
				locTarget = livingEntity.getLocation();
			} else {
				Block b;
				try {
					b = getTargetedBlock(livingEntity, power);
					if (b != null && !BlockUtils.isAir(b.getType())) {
						locTarget = b.getLocation();
						locTarget.add(0.5, 0, 0.5);
					}
				} catch (IllegalStateException e) {
					DebugHandler.debugIllegalState(e);
				}
			}
			if (locTarget == null && entTarget == null) return noTarget(livingEntity);
			if (locTarget != null) {
				locTarget.setY(locTarget.getY() + yOffset);
				locTarget.setDirection(livingEntity.getLocation().getDirection());
			}
			
			boolean somethingWasDone = runSpells(livingEntity, entTarget, locTarget, power);
			if (!somethingWasDone) return noTarget(livingEntity);
			
			if (entTarget != null) {
				sendMessages(livingEntity, entTarget);
				return PostCastAction.NO_MESSAGES;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		return runSpells(caster, null, target, power);
	}
	
	@Override
	public boolean castAtLocation(Location location, Power power) {
		return runSpells(null, null, location, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		return runSpells(caster, target, null, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		return runSpells(null, target, null, power);
	}
	
	private boolean runSpells(LivingEntity livingEntity, LivingEntity entTarget, Location locTarget, Power power) {
		boolean somethingWasDone = false;
		if (!castRandomSpellInstead) {
			int delay = 0;
			Subspell spell;
			List<DelayedSpell> delayedSpells = new ArrayList<>();
			for (Action action : actions) {
				if (action.isDelay()) {
					delay += action.getDelay();
				} else if (action.isSpell()) {
					spell = action.getSpell();
					if (delay == 0) {
						boolean ok = castTargetedSpell(spell, livingEntity, entTarget, locTarget, power);
						if (ok) somethingWasDone = true;
						else if (stopOnFail) break;
					} else {
						DelayedSpell ds = new DelayedSpell(spell, livingEntity, entTarget, locTarget, power, delayedSpells);
						delayedSpells.add(ds);
						MagicSpells.scheduleDelayedTask(ds, delay);
						somethingWasDone = true;
					}
				}
			}
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isSpell()) somethingWasDone = castTargetedSpell(action.getSpell(), livingEntity, entTarget, locTarget, power);
			else somethingWasDone = false;
		}
		if (somethingWasDone) {
			if (livingEntity != null) {
				if (entTarget != null) playSpellEffects(livingEntity, entTarget);
				else if (locTarget != null) playSpellEffects(livingEntity, locTarget);
			} else {
				if (entTarget != null) playSpellEffects(EffectPosition.TARGET, entTarget);
				else if (locTarget != null) playSpellEffects(EffectPosition.TARGET, locTarget);
			}
		}
		return somethingWasDone;
	}
	
	private boolean castTargetedSpell(Subspell spell, LivingEntity caster, LivingEntity entTarget, Location locTarget, Power power) {
		boolean success = false;
		if (spell.isTargetedEntitySpell() && entTarget != null) {
			success = spell.castAtEntity(caster, entTarget, power);
		} else if (spell.isTargetedLocationSpell()) {
			if (entTarget != null) success = spell.castAtLocation(caster, entTarget.getLocation(), power);
			else if (locTarget != null) success = spell.castAtLocation(caster, locTarget, power);
		} else {
			success = spell.cast(caster, power) == PostCastAction.HANDLE_NORMALLY;
		}
		return success;
	}

	public List<Spell> getAllSpell() {
		return actions.stream().map(a -> a.spell.getSpell()).collect(Collectors.toList());
	}
	private static class Action {
		
		private Subspell spell;
		private int delay;
		
		Action(Subspell spell) {
			this.spell = spell;
			delay = 0;
		}
		
		Action(int delay) {
			this.delay = delay;
			spell = null;
		}
		
		public boolean isSpell() {
			return spell != null;
		}
		
		public Subspell getSpell() {
			return spell;
		}
		
		public boolean isDelay() {
			return delay > 0;
		}
		
		public int getDelay() {
			return delay;
		}
		
	}
	
	private class DelayedSpell implements Runnable {
		
		private Subspell spell;
		private LivingEntity caster;
		private LivingEntity entTarget;
		private Location locTarget;
		private Power power;
		
		private List<DelayedSpell> delayedSpells;
		private boolean cancelled;
		
		DelayedSpell(Subspell spell, LivingEntity caster, LivingEntity entTarget, Location locTarget, Power power, List<DelayedSpell> delayedSpells) {
			this.spell = spell;
			this.caster = caster;
			this.entTarget = entTarget;
			this.locTarget = locTarget;
			this.power = power;
			this.delayedSpells = delayedSpells;
			cancelled = false;
		}
		
		public void cancel() {
			cancelled = true;
			delayedSpells = null;
		}
		
		public void cancelAll() {
			for (DelayedSpell ds : delayedSpells) {
				if (ds == this) continue;
				ds.cancel();
			}
			delayedSpells.clear();
			cancel();
		}
		
		@Override
		public void run() {
			if (!cancelled) {
				if (caster == null || caster.isValid()) {
					boolean ok = castTargetedSpell(spell, caster, entTarget, locTarget, power);
					delayedSpells.remove(this);
					if (!ok && stopOnFail) cancelAll();
				} else cancelAll();
			}
			delayedSpells = null;
		}
		
	}
	
}
