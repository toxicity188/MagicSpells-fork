package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.nisovin.magicspells.power.Power;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.passive.PassiveManager;
import com.nisovin.magicspells.spells.passive.PassiveTrigger;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class PassiveSpell extends Spell implements SpellSupplier {

	private static PassiveManager manager;
	
	private final Random random = new Random();

	private final float chance;

	private final int delay;

	private boolean disabled = false;
	private final boolean ignoreCancelled;
	private final boolean castWithoutTarget;
	private final boolean sendFailureMessages;
	private final boolean cancelDefaultAction;
	private final boolean requireCancelledEvent;
	private final boolean cancelDefaultActionWhenCastFails;

	private final List<String> triggers;
	private final List<String> spellNames;
	private List<Subspell> spells;

	@Override
	public List<Spell> getSubSpells() {
		return spells.stream().map(Subspell::getSpell).collect(Collectors.toList());
	}

	public PassiveSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		if (manager == null) manager = new PassiveManager();

		chance = getConfigFloat("chance", 100F) / 100F;

		delay = getConfigInt("delay", -1);

		ignoreCancelled = getConfigBoolean("ignore-cancelled", true);
		castWithoutTarget = getConfigBoolean("cast-without-target", false);
		sendFailureMessages = getConfigBoolean("send-failure-messages", false);
		cancelDefaultAction = getConfigBoolean("cancel-default-action", false);
		requireCancelledEvent = getConfigBoolean("require-cancelled-event", false);
		cancelDefaultActionWhenCastFails = getConfigBoolean("cancel-default-action-when-cast-fails", false);

		triggers = getConfigStringList("triggers", null);
		spellNames = getConfigStringList("spells", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		// Create spell list
		spells = new ArrayList<>();
		if (spellNames != null) {
			for (String spellName : spellNames) {
				Subspell spell = new Subspell(spellName);
				if (!spell.process()) continue;
				spells.add(spell);
			}
		}
		if (spells.isEmpty()) {
			MagicSpells.error("PassiveSpell '" + internalName + "' has no spells defined!");
			return;
		}

		// Get trigger
		int trigCount = 0;
		if (triggers != null) {
			for (String strigger : triggers) {
				String type = strigger;
				String var = null;
				if (strigger.contains(" ")) {
					String[] data = Util.splitParams(strigger, 2);
					type = data[0];
					var = data[1];
				}
				type = type.toLowerCase();

				PassiveTrigger trigger = PassiveTrigger.getByName(type);
				if (trigger != null) {
					manager.registerSpell(this, trigger, var);
					trigCount++;
				} else {
					MagicSpells.error("PassiveSpell '" + internalName + "' has an invalid trigger defined: " + strigger);
				}
			}
		}
		if (trigCount == 0) MagicSpells.error("PassiveSpell '" + internalName + "' has no triggers defined!");
	}
	
	public static PassiveManager getManager() {
		return manager;
	}
	
	public List<Subspell> getActivatedSpells() {
		return spells;
	}

	public boolean cancelDefaultAction() {
		return cancelDefaultAction;
	}

	public boolean cancelDefaultActionWhenCastFails() {
		return cancelDefaultActionWhenCastFails;
	}

	public boolean ignoreCancelled() {
		return ignoreCancelled;
	}

	public boolean requireCancelledEvent() {
		return requireCancelledEvent;
	}

	@Override
	public boolean canBind(CastItem item) {
		return false;
	}

	@Override
	public boolean canCastWithItem() {
		return false;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}

	public static void resetManager() {
		if (manager == null) return;
		manager.turnOff();
		manager = null;
	}

	private boolean isActuallyNonTargeted(Spell spell) {
		if (spell instanceof ExternalCommandSpell) return !((ExternalCommandSpell) spell).requiresPlayerTarget();
		if (spell instanceof BuffSpell) return !((BuffSpell) spell).isTargeted();
		return false;
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}
	
	public boolean activate(Player caster) {
		return activate(caster, null, null);
	}
	
	public boolean activate(Player caster, Power power) {
		return activate(caster, null, null, power);
	}
	
	public boolean activate(Player caster, LivingEntity target) {
		return activate(caster, target, null, new Power(1));
	}
	
	public boolean activate(Player caster, Location location) {
		return activate(caster, null, location, new Power(1));
	}
	
	public boolean activate(final Player caster, final LivingEntity target, final Location location) {
		return activate(caster, target, location, new Power(1));
	}
	
	public boolean activate(final Player caster, final LivingEntity target, final Location location, final Power power) {
		if (delay < 0) return activateSpells(caster, target, location, power);
		MagicSpells.scheduleDelayedTask(() -> activateSpells(caster, target, location, power), delay);
		return false;
	}
	
	// DEBUG INFO: level 3, activating passive spell spellname for player playername state state
	// DEBUG INFO: level 3, casting spell effect spellname
	// DEBUG INFO: level 3, casting without target
	// DEBUG INFO: level 3, casting at entity
	// DEBUG INFO: level 3, target cancelled (TE)
	// DEBUG INFO: level 3, casting at location
	// DEBUG INFO: level 3, target cancelled (TL)
	// DEBUG INFO: level 3, casting normally
	// DEBUG INFO: level 3, target cancelled (UE)
	// DEBUG INFO: level 3, target cancelled (UL)
	// DEBUG INFO: level 3, passive spell cancelled
	private boolean activateSpells(Player caster, LivingEntity target, Location location, Power basePower) {
		SpellCastState state = getCastState(caster);
		MagicSpells.debug(3, "Activating passive spell '" + name + "' for player " + caster.getName() + " (state: " + state + ')');
		if (state != SpellCastState.NORMAL && sendFailureMessages) {
			if (state == SpellCastState.ON_COOLDOWN) {
				MagicSpells.sendMessage(formatMessage(strOnCooldown, "%c", Math.round(getCooldown(caster)) + ""), caster, null);
			} else if (state == SpellCastState.MISSING_REAGENTS) {
				MagicSpells.sendMessage(strMissingReagents, caster, MagicSpells.NULL_ARGS);
				if (MagicSpells.showStrCostOnMissingReagents() && strCost != null && !strCost.isEmpty()) {
					MagicSpells.sendMessage("    (" + strCost + ')', caster, MagicSpells.NULL_ARGS);
				}
			}
			return false;
		}

		if (disabled || (chance < 0.999 && random.nextFloat() > chance) || state != SpellCastState.NORMAL) return false;

		disabled = true;
		SpellCastEvent event = new SpellCastEvent(this, caster, SpellCastState.NORMAL, basePower, null, cooldown, reagents.clone(), 0);
		EventUtil.call(event);
		if (event.isCancelled() || event.getSpellCastState() != SpellCastState.NORMAL) {
			MagicSpells.debug(3, "   Passive spell cancelled");
			disabled = false;
			return false;
		}
		if (event.haveReagentsChanged() && !hasReagents(caster, event.getReagents())) {
			disabled = false;
			return false;
		}
		setCooldown(caster, event.getCooldown());
		basePower = event.getPower();
		boolean spellEffectsDone = false;
		for (Subspell spell : spells) {
			MagicSpells.debug(3, "    Casting spell effect '" + spell.getSpell().getName() + '\'');
			if (castWithoutTarget) {
				MagicSpells.debug(3, "    Casting without target");
				spell.cast(caster, basePower);
				if (!spellEffectsDone) {
					playSpellEffects(EffectPosition.CASTER, caster);
					spellEffectsDone = true;
				}
			} else if (spell.isTargetedEntitySpell() && target != null && !isActuallyNonTargeted(spell.getSpell())) {
				MagicSpells.debug(3, "    Casting at entity");
				SpellTargetEvent targetEvent = new SpellTargetEvent(this, caster, target, basePower);
				EventUtil.call(targetEvent);
				if (!targetEvent.isCancelled()) {
					target = targetEvent.getTarget();
					spell.castAtEntity(caster, target, targetEvent.getPower());
					if (!spellEffectsDone) {
						playSpellEffects(caster, target);
						spellEffectsDone = true;
					}
				} else MagicSpells.debug(3, "      Target cancelled (TE)");
			} else if (spell.isTargetedLocationSpell() && (location != null || target != null)) {
				MagicSpells.debug(3, "    Casting at location");
				Location loc = null;
				if (location != null) loc = location;
				else if (target != null) loc = target.getLocation();
				if (loc != null) {
					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, caster, loc, basePower);
					EventUtil.call(targetEvent);
					if (!targetEvent.isCancelled()) {
						loc = targetEvent.getTargetLocation();
						spell.castAtLocation(caster, loc, targetEvent.getPower());
						if (!spellEffectsDone) {
							playSpellEffects(caster, loc);
							spellEffectsDone = true;
						}
					} else MagicSpells.debug(3, "      Target cancelled (TL)");
				}
			} else {
				MagicSpells.debug(3, "    Casting normally");
				Power power = basePower;
				if (target != null) {
					SpellTargetEvent targetEvent = new SpellTargetEvent(this, caster, target, power);
					EventUtil.call(targetEvent);
					if (!targetEvent.isCancelled()) {
						power = targetEvent.getPower();
					} else {
						MagicSpells.debug(3, "      Target cancelled (UE)");
						continue;
					}
				} else if (location != null) {
					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, caster, location, basePower);
					EventUtil.call(targetEvent);
					if (!targetEvent.isCancelled()) {
						power = targetEvent.getPower();
					} else {
						MagicSpells.debug(3, "      Target cancelled (UL)");
						continue;
					}
				}
				spell.cast(caster, power);
				if (!spellEffectsDone) {
					playSpellEffects(EffectPosition.CASTER, caster);
					spellEffectsDone = true;
				}
			}
		}

		removeReagents(caster, event.getReagents());
		sendMessage(strCastSelf, caster, MagicSpells.NULL_ARGS);
		SpellCastedEvent event2 = new SpellCastedEvent(this, caster, SpellCastState.NORMAL, basePower, null, event.getCooldown(), event.getReagents(), PostCastAction.HANDLE_NORMALLY);
		EventUtil.call(event2);
		disabled = false;
		return true;
	}

}
