package com.nisovin.magicspells.spells;

import java.util.*;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.power.Power;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.BuffManager;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spelleffects.EffectTracker;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public abstract class BuffSpell extends TargetedSpell implements TargetedEntitySpell {

	protected BuffSpell thisSpell;

	protected Map<LivingEntity, Integer> useCounter;
	protected Map<LivingEntity, Long> durationEndTime;

	protected ValidTargetList targetList;

	protected float duration;

	protected int numUses;
	protected int useCostInterval;

	protected SpellReagents reagents;

	protected boolean toggle;
	protected boolean targeted;
	protected boolean castWithItem;
	protected boolean castByCommand;
	protected boolean cancelOnJoin;
	protected boolean cancelOnDeath;
	protected boolean cancelOnLogout;
	protected boolean cancelOnTeleport;
	protected boolean cancelOnSpellCast;
	protected boolean cancelOnTakeDamage;
	protected boolean cancelOnGiveDamage;
	protected boolean cancelOnChangeWorld;
	protected boolean powerAffectsDuration;

	protected String strFade;
	protected String spellOnEndName;
	protected String spellOnCostName;
	protected String spellOnUseIncrementName;

	protected SpellFilter filter;

	protected Subspell spellOnEnd;
	protected Subspell spellOnCost;
	protected Subspell spellOnUseIncrement;


	public BuffSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		thisSpell = this;

		targetList = new ValidTargetList(this, getConfigStringList("can-target", null));
		targetList.enforce(ValidTargetList.TargetingElement.TARGET_PLAYERS, true);

		duration = getConfigFloat("duration", 0);

		numUses = getConfigInt("num-uses", 0);
		useCostInterval = getConfigInt("use-cost-interval", 0);

		reagents = getConfigReagents("use-cost");

		toggle = getConfigBoolean("toggle", true);
		targeted = getConfigBoolean("targeted", false);
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
		cancelOnJoin = getConfigBoolean("cancel-on-join", false);
		cancelOnDeath = getConfigBoolean("cancel-on-death", false);
		cancelOnLogout = getConfigBoolean("cancel-on-logout", false);
		cancelOnTeleport = getConfigBoolean("cancel-on-teleport", false);
		cancelOnSpellCast = getConfigBoolean("cancel-on-spell-cast", false);
		cancelOnTakeDamage = getConfigBoolean("cancel-on-take-damage", false);
		cancelOnGiveDamage = getConfigBoolean("cancel-on-give-damage", false);
		cancelOnChangeWorld = getConfigBoolean("cancel-on-change-world", false);
		powerAffectsDuration = getConfigBoolean("power-affects-duration", true);

		strFade = getConfigString("str-fade", "");
		spellOnEndName = getConfigString("spell-on-end", "");
		spellOnCostName = getConfigString("spell-on-cost", "");
		spellOnUseIncrementName = getConfigString("spell-on-use-increment", "");

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> tagList = getConfigStringList("spell-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-spell-tags", null);
		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);

		if (cancelOnGiveDamage || cancelOnTakeDamage) registerEvents(new DamageListener());
		if (cancelOnDeath) registerEvents(new DeathListener());
		if (cancelOnTeleport) registerEvents(new TeleportListener());
		if (cancelOnChangeWorld) registerEvents(new ChangeWorldListener());
		if (cancelOnSpellCast) registerEvents(new SpellCastListener());
		if (cancelOnLogout) registerEvents(new QuitListener());
		if (cancelOnJoin) registerEvents(new JoinListener());
		registerEvents(new EntityListener());

		if (numUses > 0 || (reagents != null && useCostInterval > 0)) useCounter = new HashMap<>();
		if (duration > 0) durationEndTime = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		// Check spell on use increment
		spellOnUseIncrement = new Subspell(spellOnUseIncrementName);
		if (!spellOnUseIncrement.process()) {
			if (!spellOnUseIncrementName.isEmpty()) MagicSpells.error("BuffSpell '" + internalName + "' has an invalid spell-on-use-increment defined!");
			spellOnUseIncrement = null;
		}

		// Check spell on cost
		spellOnCost = new Subspell(spellOnCostName);
		if (!spellOnCost.process()) {
			if (!spellOnCostName.isEmpty()) MagicSpells.error("BuffSpell '" + internalName + "' has an invalid spell-on-cost defined!");
			spellOnCost = null;
		}

		// Check spell on end
		spellOnEnd = new Subspell(spellOnEndName);
		if (!spellOnEnd.process()) {
			if (!spellOnEndName.isEmpty()) MagicSpells.error("BuffSpell '" + internalName + "' has an invalid spell-on-end defined!");
			spellOnEnd = null;
		}

	}

	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}

	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}

	@Override
	public final PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		LivingEntity target;

		if (targeted) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			if (!targetList.canTarget(targetInfo.getTarget())) return noTarget(livingEntity);

			target = targetInfo.getTarget();
			power = targetInfo.getPower();
		} else {
			target = livingEntity;
		}

		PostCastAction action = activate(livingEntity, target, power, args, state == SpellCastState.NORMAL);
		if (targeted && action == PostCastAction.HANDLE_NORMALLY) {
			sendMessages(livingEntity, target);
			return PostCastAction.NO_MESSAGES;
		}

		return action;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		return activate(caster, target, power, MagicSpells.NULL_ARGS, true) == PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		return activate(null, target, power, MagicSpells.NULL_ARGS, true) == PostCastAction.HANDLE_NORMALLY;
	}

	private PostCastAction activate(LivingEntity caster, LivingEntity target, Power power, String[] args, boolean normal) {
		if (isActive(target) && toggle) {
			turnOff(target);
			return PostCastAction.ALREADY_HANDLED;
		}

		if (!normal) return PostCastAction.HANDLE_NORMALLY;

		boolean ok;
		if (isActive(target)) ok = recastBuff(target, power, args);
		else ok = castBuff(target, power, args);
		if (!ok) return PostCastAction.HANDLE_NORMALLY;

		startSpellDuration(target, power);
		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);

		return PostCastAction.HANDLE_NORMALLY;
	}

	public abstract boolean castBuff(LivingEntity entity, Power power, String[] args);

	public boolean recastBuff(LivingEntity entity, Power power, String[] args) {
		stopEffects();
		return true;
	}

	public void setAsEverlasting() {
		duration = 0;
		numUses = 0;
		useCostInterval = 0;
	}

	/**
	 * Begins counting the spell duration for a player
	 * @param livingEntity the player to begin counting duration
	 */
	private void startSpellDuration(final LivingEntity livingEntity, Power power) {
		if (duration > 0 && durationEndTime != null) {

			float dur = duration;
			if (powerAffectsDuration) dur *= power.intValue();
			durationEndTime.put(livingEntity, System.currentTimeMillis() + Math.round(dur * TimeUtil.MILLISECONDS_PER_SECOND));

			MagicSpells.scheduleDelayedTask(() -> {
				if (isExpired(livingEntity)) turnOff(livingEntity);
			}, Math.round(dur * TimeUtil.TICKS_PER_SECOND) + 1); // overestimate ticks, since the duration is real-time ms based
		}

		playSpellEffectsBuff(livingEntity, entity -> thisSpell.isActiveAndNotExpired((LivingEntity) entity));

		BuffManager manager = MagicSpells.getBuffManager();
		if (manager != null) manager.addBuff(livingEntity, this);
	}

	/**
	 * Checks whether the spell's duration has expired for a player
	 * @param entity the player to check
	 * @return true if the spell has expired, false otherwise
	 */
	protected boolean isExpired(LivingEntity entity) {
		if (duration <= 0 || durationEndTime == null) return false;
		if (entity == null) return false;
		Long endTime = durationEndTime.get(entity);
		if (endTime == null) return false;
		return endTime <= System.currentTimeMillis();
	}

	public boolean isActiveAndNotExpired(LivingEntity entity) {
		if (duration > 0 && isExpired(entity)) return false;
		return isActive(entity);
	}

	/**
	 * Checks if this buff spell is active for the specified player
	 * @param entity the player to check
	 * @return true if the spell is active, false otherwise
	 */
	public abstract boolean isActive(LivingEntity entity);

	/**
	 * Adds a use to the spell for the player. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * This does not automatically charge the use cost.
	 * @param entity the player to add the use for
	 * @return the player's current number of uses (returns 0 if the use counting feature is disabled)
	 */
	protected int addUse(LivingEntity entity) {
		// Run spell on use increment first thing in case we want to intervene
		if (spellOnUseIncrement != null && entity instanceof Player) spellOnUseIncrement.cast((Player) entity, new Power(1));

		if (numUses > 0 || (reagents != null && useCostInterval > 0)) {

			Integer uses = useCounter.get(entity);

			if (uses == null) uses = 1;
			else uses++;

			if (numUses > 0 && uses >= numUses) turnOff(entity);
			else useCounter.put(entity, uses);

			return uses;
		}

		return 0;

	}

	/**
	 * Removes this spell's use cost from the player's inventory. If the reagents aren't available, the spell will expire.
	 * @param entity the player to remove the cost from
	 * @return true if the reagents were removed, or if the use cost is disabled, false otherwise
	 */
	protected boolean chargeUseCost(LivingEntity entity) {
		// Run spell on cost first thing to dodge the early returns and allow intervention
		if (spellOnCost != null) spellOnCost.cast(entity, new Power(1));

		if (reagents == null) return true;
		if (useCostInterval <= 0) return true;
		if (useCounter == null) return true;

		Integer uses = useCounter.get(entity);
		if (uses == null) return true;
		if (uses % useCostInterval != 0) return true;

		if (hasReagents(entity, reagents)) {
			removeReagents(entity, reagents);
			return true;
		}

		if (!hasReagents(entity, reagents)) {
			turnOff(entity);
			return false;
		}

		return true;
	}

	/**
	 * Adds a use to the spell for the player. If the number of uses exceeds the amount allowed, the spell will immediately expire.
	 * Removes this spell's use cost from the player's inventory. This does not return anything, to get useful return values, use
	 * addUse() and chargeUseCost().
	 * @param entity the player to add a use and charge cost to
	 */
	protected void addUseAndChargeCost(LivingEntity entity) {
		addUse(entity);
		chargeUseCost(entity);
	}

	/**
	 * Turns off this spell for the specified player. This can be called from many situations, including when the spell expires or the uses run out.
	 * When overriding this function, you should always be sure to call super.turnOff(player).
	 * @param entity
	 */
	public final void turnOff(LivingEntity entity) {
		if (!isActive(entity)) return;

		if (useCounter != null) useCounter.remove(entity);
		if (durationEndTime != null) durationEndTime.remove(entity);

		BuffManager manager = MagicSpells.getBuffManager();
		if (manager != null) manager.removeBuff(entity, this);

		turnOffBuff(entity);
		playSpellEffects(EffectPosition.DISABLED, entity);
		cancelEffects(EffectPosition.CASTER, entity.getUniqueId().toString());
		stopEffects();

		if (!(entity instanceof Player)) return;
		sendMessage(strFade, entity, null);
		if (spellOnEnd != null) spellOnEnd.cast(entity, new Power(1));
	}

	public void stopEffects() {
		Set<EffectTracker> trackers = new HashSet<>(getEffectTrackers());
		getEffectTrackers().clear();
		for (EffectTracker effectTracker : trackers) {
			effectTracker.stop();
		}
		trackers.clear();
	}

	protected abstract void turnOffBuff(LivingEntity entity);

	@Override
	protected abstract void turnOff();

	@Override
	public boolean isBeneficialDefault() {
		return true;
	}

	public boolean isTargeted() {
		return targeted;
	}

	public class DamageListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onPlayerDamage(EntityDamageEvent e) {
			Entity entity = e.getEntity();
			if (cancelOnTakeDamage && entity instanceof LivingEntity && isActiveAndNotExpired((LivingEntity) entity)) {
				turnOff((LivingEntity) entity);
			} else if (cancelOnGiveDamage && e instanceof EntityDamageByEntityEvent) {
				EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) e;
				Entity damager = evt.getDamager();
				if (damager instanceof LivingEntity && isActiveAndNotExpired((LivingEntity) damager)) {
					turnOff((LivingEntity) damager);
				} else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity) {
					LivingEntity shooter = (LivingEntity) ((Projectile) damager).getShooter();
					if (isActiveAndNotExpired(shooter)) turnOff(shooter);
				}
			}
		}

	}

	public class EntityListener implements Listener {

		@EventHandler
		public void onEntityDeath(EntityDeathEvent event) {
			LivingEntity entity = event.getEntity();
			if (entity instanceof Player) return;
			if (!isActiveAndNotExpired(entity)) return;
			turnOff(entity);
		}

	}

	public class DeathListener implements Listener {

		@EventHandler
		public void onPlayerDeath(PlayerDeathEvent event) {
			Player pl = event.getEntity();
			if (!isActiveAndNotExpired(pl)) return;
			turnOff(pl);
		}

	}

	public class TeleportListener implements Listener {

		@EventHandler(priority=EventPriority.LOWEST)
		public void onTeleport(EntityTeleportEvent e) {
			if (!(e.getEntity() instanceof LivingEntity)) return;
			LivingEntity entity = (LivingEntity) e.getEntity();
			if (!isActiveAndNotExpired(entity)) return;

			Location locationFrom = e.getFrom();
			Location locationTo = e.getTo();

			if (LocationUtil.differentWorldDistanceGreaterThan(locationFrom, locationTo, 5)) {
				turnOff(entity);
			}

		}

	}

	public class ChangeWorldListener implements Listener {

		@EventHandler(priority=EventPriority.LOWEST)
		public void onChangeWorld(PlayerChangedWorldEvent e) {
			Player pl = e.getPlayer();
			if (pl == null) return;
			if (isActiveAndNotExpired(pl)) turnOff(pl);
		}

	}

	public class SpellCastListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent e) {
			if (thisSpell != e.getSpell() && e.getSpellCastState() == SpellCastState.NORMAL && isActiveAndNotExpired(e.getCaster())) {
				if (filter.check(e.getSpell())) turnOff(e.getCaster());
			}
		}

	}

	public class QuitListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR)
		public void onPlayerQuit(PlayerQuitEvent e) {
			Player pl = e.getPlayer();
			if (pl == null) return;
			if (isActiveAndNotExpired(pl)) turnOff(pl);
		}

	}

	public class JoinListener implements Listener {

		@EventHandler
		public void onJoin(PlayerJoinEvent e) {
			Player pl = e.getPlayer();
			if (pl == null) return;
			if (isActiveAndNotExpired(pl)) turnOff(pl);
		}

	}

}
