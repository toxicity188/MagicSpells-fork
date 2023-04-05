package com.nisovin.magicspells.spells;

import java.util.UUID;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public final class MultiSpell extends InstantSpell implements SpellSupplier {
	
	private static final Pattern RANGED_DELAY_PATTERN = Pattern.compile("DELAY [0-9]+ [0-9]+");
	private static final Pattern BASIC_DELAY_PATTERN = Pattern.compile("DELAY [0-9]+");

	private Random random;

	private List<String> spellList;
	private List<ActionChance> actions;

	private boolean castWithItem;
	private boolean castByCommand;
	private boolean customSpellCastChance;
	private boolean castRandomSpellInstead;
	private boolean enableIndividualChances;

	public MultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		random = new Random();

		actions = new ArrayList<>();
		spellList = getConfigStringList("spells", null);

		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
		customSpellCastChance = getConfigBoolean("enable-custom-spell-cast-chance", false);
		castRandomSpellInstead = getConfigBoolean("cast-random-spell-instead", false);
		enableIndividualChances = getConfigBoolean("enable-individual-chances", false);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (spellList != null) {
			for (String s : spellList) {
				String[] parts = s.split(":");
				double chance = parts.length == 2 ? Double.parseDouble(parts[1]) : 0.0D;
				s = parts[0];
				if (RegexUtil.matches(RANGED_DELAY_PATTERN, s)) {
					String[] splits = s.split(" ");
					int minDelay = Integer.parseInt(splits[1]);
					int maxDelay = Integer.parseInt(splits[2]);
					actions.add(new ActionChance(new Action(minDelay, maxDelay), chance));
				} else if (RegexUtil.matches(BASIC_DELAY_PATTERN, s)) {
					int delay = Integer.parseInt(s.split(" ")[1]);
					actions.add(new ActionChance(new Action(delay), chance));
				} else {
					Subspell spell = new Subspell(s);
					if (spell.process()) actions.add(new ActionChance(new Action(spell), chance));
					else MagicSpells.error("MultiSpell '" + internalName + "' has an invalid spell '" + s + "' defined!");
				}
			}
		}
		spellList = null;
	}

	@Override
	public Spell.PostCastAction castSpell(LivingEntity livingEntity, Spell.SpellCastState state, float power, String[] args) {
		if (state == Spell.SpellCastState.NORMAL) {
			if (!castRandomSpellInstead) {
				int delay = 0;
				for (ActionChance actionChance : actions) {
					Action action = actionChance.getAction();
					if (action.isDelay()) {
						delay += action.getDelay();
					} else if (action.isSpell()) {
						Subspell spell = action.getSpell();
						if (delay == 0) spell.cast(livingEntity, power);
						else MagicSpells.scheduleDelayedTask(new DelayedSpell(spell, livingEntity, power), delay);
					}
				}
			} else {
				int index;
				if (customSpellCastChance) {
					int total = 0;
					for (ActionChance actionChance : actions) {
						total = (int) Math.round(total + actionChance.getChance());
					}
					index = random.nextInt(total);
					int s = 0;
					int i = 0;
					while (s < index) {
						s = (int) Math.round(s + actions.get(i++).getChance());
					}
					Action action = actions.get(Math.max(0, i - 1)).getAction();
					if (action.isSpell()) action.getSpell().cast(livingEntity, power);
				} else if (enableIndividualChances) {
					for (ActionChance actionChance : actions) {
						double chance = Math.random();
						if ((actionChance.getChance() / 100.0D > chance) && actionChance.getAction().isSpell()) {
							Action action = actionChance.getAction();
							action.getSpell().cast(livingEntity, power);
						}
					}
				} else {
					Action action = actions.get(random.nextInt(actions.size())).getAction();
					action.getSpell().cast(livingEntity, power);
				}
			}
			playSpellEffects(EffectPosition.CASTER, livingEntity);
		}
		return Spell.PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(final CommandSender sender, final String[] args) {
		if (!castRandomSpellInstead) {
			int delay = 0;
			for (ActionChance actionChance : actions) {
				Action action = actionChance.getAction();
				if (action.isSpell()) {
					if (delay == 0) action.getSpell().getSpell().castFromConsole(sender, args);
					else {
						final Spell spell = action.getSpell().getSpell();
						MagicSpells.scheduleDelayedTask(() -> spell.castFromConsole(sender, args), delay);
					}
				} else if (action.isDelay()) delay += action.getDelay();
			}
		} else {
			int index;
			if (customSpellCastChance) {
				int total = 0;
				for (ActionChance actionChance : actions) {
					total = (int) Math.round(total + actionChance.getChance());
				}
				index = random.nextInt(total);
				int s = 0;
				int i = 0;
				while (s < index) {
					s = (int) Math.round(s + actions.get(i++).getChance());
				}
				Action action = actions.get(Math.max(0, i - 1)).getAction();
				if (action.isSpell()) action.getSpell().getSpell().castFromConsole(sender, args);
			} else if (enableIndividualChances) {
				for (ActionChance actionChance : actions) {
					double chance = Math.random();
					if ((actionChance.getChance() / 100.0D > chance) && actionChance.getAction().isSpell()) {
						actionChance.getAction().getSpell().getSpell().castFromConsole(sender, args);
					}
				}
			} else {
				Action action = actions.get(random.nextInt(actions.size())).getAction();
				if (action.isSpell()) action.getSpell().getSpell().castFromConsole(sender, args);
			}
		}
		return true;
	}

	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}

	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}

	public List<Spell> getSubSpells() {
		return actions.stream().map(a -> a.action.spell.getSpell()).collect(Collectors.toList());
	}

	private class Action {
		
		private Subspell spell;
		private int delay; // Also gonna serve as minimum delay
		private boolean isRangedDelay = false;
		private int maxDelay;

		Action(Subspell spell) {
			this.spell = spell;
			delay = 0;
		}

		Action(int delay) {
			this.delay = delay;
			spell = null;
		}
		
		Action(int minDelay, int maxDelay) {
			this.delay = minDelay;
			this.maxDelay = maxDelay;
			spell = null;
			isRangedDelay = true;
		}

		public boolean isSpell() {
			return spell != null;
		}

		public Subspell getSpell() {
			return spell;
		}

		public boolean isDelay() {
			return delay > 0 || isRangedDelay;
		}

		private int getRandomDelay() {
			return random.nextInt(maxDelay - delay) + delay;
		}
		
		public int getDelay() {
			return isRangedDelay ? getRandomDelay() : delay;
		}
		
	}

	private static class DelayedSpell implements Runnable {
		
		private Subspell spell;
		private UUID casterUUID;
		private float power;

		DelayedSpell(Subspell spell, LivingEntity livingEntity, float power) {
			this.spell = spell;
			this.casterUUID = livingEntity.getUniqueId();
			this.power = power;
		}

		@Override
		public void run() {
			Entity entity = Bukkit.getEntity(casterUUID);
			if (entity != null && entity.isValid() && entity instanceof LivingEntity) spell.cast((LivingEntity) entity, power);
		}
		
	}

	private static class ActionChance {
		
		private Action action;
		private double chance;

		ActionChance(Action action, double chance) {
			this.action = action;
			this.chance = chance;
		}

		public Action getAction() {
			return action;
		}

		public double getChance() {
			return chance;
		}
		
	}
	
}
