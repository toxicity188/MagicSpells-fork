package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PortalSpell extends InstantSpell {

	private String firstMarkSpellName;
	private String secondMarkSpellName;

	private MarkSpell firstMark;
	private MarkSpell secondMark;

	private SpellReagents teleportCost;

	private int duration;
	private int minDistanceSq;
	private int maxDistanceSq;
	private int effectInterval;
	private int teleportCooldown;

	private float vertRadius;
	private float horizRadius;

	private boolean allowReturn;
	private boolean tpOtherPlayers;
	private boolean usingSecondMarkSpell;
	private boolean chargeCostToTeleporter;

	private String strNoMark;
	private String strTooFar;
	private String strTooClose;
	private String strTeleportCostFail;
	private String strTeleportCooldownFail;

	public PortalSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		firstMarkSpellName = getConfigString("mark-spell", "");
		secondMarkSpellName = getConfigString("second-mark-spell", "");

		teleportCost = getConfigReagents("teleport-cost");

		duration = getConfigInt("duration", 400);
		minDistanceSq = getConfigInt("min-distance", 10);
		maxDistanceSq = getConfigInt("max-distance", 0);
		effectInterval = getConfigInt("effect-interval", 10);
		teleportCooldown = getConfigInt("teleport-cooldown", 5) * 1000;

		horizRadius = getConfigFloat("horiz-radius", 1F);
		vertRadius = getConfigFloat("vert-radius", 1F);

		allowReturn = getConfigBoolean("allow-return", true);
		tpOtherPlayers = getConfigBoolean("teleport-other-players", true);
		chargeCostToTeleporter = getConfigBoolean("charge-cost-to-teleporter", false);

		strNoMark = getConfigString("str-no-mark", "You have not marked a location to make a portal to.");
		strTooFar = getConfigString("str-too-far", "You are too far away from your marked location.");
		strTooClose = getConfigString("str-too-close", "You are too close to your marked location.");
		strTeleportCostFail = getConfigString("str-teleport-cost-fail", "");
		strTeleportCooldownFail = getConfigString("str-teleport-cooldown-fail", "");

		minDistanceSq *= minDistanceSq;
		maxDistanceSq *= maxDistanceSq;
	}

	@Override
	public void initialize() {
		super.initialize();

		Spell spell = MagicSpells.getSpellByInternalName(firstMarkSpellName);
		if (spell != null && spell instanceof MarkSpell) firstMark = (MarkSpell) spell;
		else MagicSpells.error("PortalSpell '" + internalName + "' has an invalid mark-spell defined!");

		usingSecondMarkSpell = false;
		if (!secondMarkSpellName.isEmpty()) {
			spell = MagicSpells.getSpellByInternalName(secondMarkSpellName);
			if (spell != null && spell instanceof MarkSpell) {
				secondMark = (MarkSpell) spell;
				usingSecondMarkSpell = true;
			} else MagicSpells.error("PortalSpell '" + internalName + "' has an invalid second-mark-spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = firstMark.getEffectiveMark(livingEntity);
			Location locSecond;
			if (loc == null) {
				sendMessage(strNoMark, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			if (usingSecondMarkSpell) {
				locSecond = secondMark.getEffectiveMark(livingEntity);
				if (locSecond == null) {
					sendMessage(strNoMark, livingEntity, args);
					return PostCastAction.ALREADY_HANDLED;
				}
			} else locSecond = livingEntity.getLocation();

			double distanceSq = 0;
			if (maxDistanceSq > 0) {
				if (!loc.getWorld().equals(locSecond.getWorld())) {
					sendMessage(strTooFar, livingEntity, args);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq > maxDistanceSq) {
						sendMessage(strTooFar, livingEntity, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
			if (minDistanceSq > 0) {
				if (loc.getWorld().equals(locSecond.getWorld())) {
					if (distanceSq == 0) distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq < minDistanceSq) {
						sendMessage(strTooClose, livingEntity, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}

			new PortalLink(this, livingEntity, loc, locSecond);
			playSpellEffects(EffectPosition.CASTER, livingEntity);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private class PortalLink implements Listener {

		private PortalSpell spell;
		private LivingEntity caster;
		private Location loc1;
		private Location loc2;
		private BoundingBox box1;
		private BoundingBox box2;
		private int taskId1 = -1;
		private int taskId2 = -1;
		private Map<String, Long> cooldownUntil;

		private PortalLink (PortalSpell spell, LivingEntity caster, Location loc1, Location loc2) {
			this.spell = spell;
			this.caster = caster;
			this.loc1 = loc1;
			this.loc2 = loc2;

			box1 = new BoundingBox(loc1, spell.horizRadius, spell.vertRadius);
			box2 = new BoundingBox(loc2, spell.horizRadius, spell.vertRadius);
			cooldownUntil = new HashMap<>();

			cooldownUntil.put(caster.getName(), System.currentTimeMillis() + spell.teleportCooldown);
			registerEvents(this);
			startTasks();
		}

		private void startTasks() {
			if (spell.effectInterval > 0) {
				taskId1 = MagicSpells.scheduleRepeatingTask(() -> {
					if (caster.isValid()) {
						playSpellEffects(EffectPosition.SPECIAL, loc1);
						playSpellEffects(EffectPosition.SPECIAL, loc2);
					} else disable();

				}, spell.effectInterval, spell.effectInterval);
			}
			taskId2 = MagicSpells.scheduleDelayedTask(this::disable, spell.duration);
		}

		@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
		private void onMove(PlayerMoveEvent event) {
			if (!tpOtherPlayers && !event.getPlayer().equals(caster)) return;
			if (!caster.isValid()) {
				disable();
				return;
			}
			Player player = event.getPlayer();
			if (box1.contains(event.getTo())) {
				if (checkTeleport(player)) {
					Location loc = loc2.clone();
					loc.setYaw(player.getLocation().getYaw());
					loc.setPitch(player.getLocation().getPitch());
					event.setTo(loc);
					playSpellEffects(EffectPosition.TARGET, player);
				}
			} else if (spell.allowReturn && box2.contains(event.getTo())) {
				if (checkTeleport(player)) {
					Location loc = loc1.clone();
					loc.setYaw(player.getLocation().getYaw());
					loc.setPitch(player.getLocation().getPitch());
					event.setTo(loc);
					playSpellEffects(EffectPosition.TARGET, player);
				}
			}
		}

		private boolean checkTeleport(LivingEntity livingEntity) {
			if (cooldownUntil.containsKey(livingEntity.getName()) && cooldownUntil.get(livingEntity.getName()) > System.currentTimeMillis()) {
				sendMessage(strTeleportCooldownFail, livingEntity, MagicSpells.NULL_ARGS);
				return false;
			}
			cooldownUntil.put(livingEntity.getName(), System.currentTimeMillis() + teleportCooldown);

			LivingEntity payer = null;
			if (spell.teleportCost != null) {
				if (spell.chargeCostToTeleporter) {
					if (hasReagents(livingEntity, spell.teleportCost)) {
						payer = livingEntity;
					} else {
						sendMessage(spell.strTeleportCostFail, livingEntity, MagicSpells.NULL_ARGS);
						return false;
					}
				} else {
					if (hasReagents(caster, spell.teleportCost)) {
						payer = caster;
					} else {
						sendMessage(spell.strTeleportCostFail, livingEntity, MagicSpells.NULL_ARGS);
						return false;
					}
				}
				if (payer == null) return false;
			}

			SpellTargetEvent event = new SpellTargetEvent(spell, caster, livingEntity, 1);
			Bukkit.getPluginManager().callEvent(event);
			if (payer != null) removeReagents(payer, spell.teleportCost);
			return true;
		}

		private void disable() {
			unregisterEvents(this);
			playSpellEffects(EffectPosition.DELAYED, loc1);
			playSpellEffects(EffectPosition.DELAYED, loc2);
			if (taskId1 > 0) MagicSpells.cancelTask(taskId1);
			if (taskId2 > 0) MagicSpells.cancelTask(taskId2);
			caster = null;
			spell = null;
			loc1 = null;
			loc2 = null;
			box1 = null;
			box2 = null;
			cooldownUntil.clear();
			cooldownUntil = null;
		}

	}

}
