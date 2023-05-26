package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.ProjectileTracker;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.ParticleProjectileHitEvent;

import de.slikey.effectlib.util.RandomUtils;

public class DodgeSpell extends BuffSpell {

	private Set<UUID> entities;

	private float distance;

	private SpellFilter filter;

	private Subspell spellBeforeDodge;
	private Subspell spellAfterDodge;
	private String spellBeforeDodgeName;
	private String spellAfterDodgeName;

	public DodgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashSet<>();

		distance = getConfigFloat("distance", 2);

		spellBeforeDodgeName = getConfigString("spell-before-dodge", "");
		spellAfterDodgeName = getConfigString("spell-after-dodge", "");

		List<String> spells = getConfigStringList("spells", null);
		List<String> deniedSpells = getConfigStringList("denied-spells", null);
		List<String> spellTags = getConfigStringList("spell-tags", null);
		List<String> deniedSpellTags = getConfigStringList("denied-spell-tags", null);

		filter = new SpellFilter(spells, deniedSpells, spellTags, deniedSpellTags);
	}

	@Override
	public void initialize() {
		super.initialize();

		spellBeforeDodge = new Subspell(spellBeforeDodgeName);
		if (!spellBeforeDodge.process() || !spellBeforeDodge.isTargetedLocationSpell()) {
			if (!spellBeforeDodgeName.isEmpty()) MagicSpells.error("DodgeSpell '" + internalName + "' has an invalid spell-before-dodge defined!");
			spellBeforeDodge = null;
		}

		spellAfterDodge = new Subspell(spellAfterDodgeName);
		if (!spellAfterDodge.process() || !spellAfterDodge.isTargetedLocationSpell()) {
			if (!spellAfterDodgeName.isEmpty()) MagicSpells.error("DodgeSpell '" + internalName + "' has an invalid spell-after-dodge defined!");
			spellAfterDodge = null;
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, Power power, String[] args) {
		entities.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		for (EffectPosition pos: EffectPosition.values()) {
			cancelEffectForAllPlayers(pos);
		}
		entities.clear();
	}

	@EventHandler
	public void onProjectileHit(ParticleProjectileHitEvent e) {
		LivingEntity target = e.getTarget();
		if (!isActive(target)) return;

		Spell spell = e.getSpell();
		if (spell != null && !filter.check(spell)) return;

		ProjectileTracker tracker = e.getTracker();
		if (tracker == null) return;
		if (tracker.getCaster().equals(target)) return;

		e.setCancelled(true);
		tracker.getImmune().add(target);
		dodge(target, tracker.getCurrentLocation());
		playSpellEffects(EffectPosition.TARGET, tracker.getCurrentLocation());
	}

	private void dodge(LivingEntity entity, Location location) {
		Location targetLoc = location.clone();
		Location entityLoc = entity.getLocation().clone();
		playSpellEffects(EffectPosition.SPECIAL, entityLoc);

		Vector v = RandomUtils.getRandomCircleVector().multiply(distance);

		targetLoc.add(v);
		targetLoc.setDirection(entity.getLocation().getDirection());

		if (entity instanceof Player && spellBeforeDodge != null) spellBeforeDodge.castAtLocation(entity, entityLoc, new Power(1));

		if (!BlockUtils.isPathable(targetLoc.getBlock().getType()) || !BlockUtils.isPathable(targetLoc.getBlock().getRelative(BlockFace.UP))) return;
		entity.teleport(targetLoc);
		addUseAndChargeCost(entity);
		playSpellEffectsTrail(entityLoc, targetLoc);
		playSpellEffects(EffectPosition.DELAYED, targetLoc);
		if (entity instanceof Player && spellAfterDodge != null) spellAfterDodge.castAtLocation(entity, targetLoc, new Power(1));
	}

}
