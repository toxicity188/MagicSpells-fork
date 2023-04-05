package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.*;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class BeamSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell, SingleSpellSupplier {

	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private double hitRadius;
	private double maxDistance;
	private double verticalHitRadius;

	private float yOffset;
	private float gravity;
	private float interval;
	private float rotation;
	private float beamVertOffset;
	private float beamHorizOffset;

	private boolean changePitch;
	private boolean stopOnHitEntity;
	private boolean stopOnHitGround;

	@Getter
	private Subspell hitSpell;
	@Getter
	private Subspell endSpell;
	@Getter
	private Subspell groundSpell;

	private String hitSpellName;
	private String endSpellName;
	private String groundSpellName;

	@Override
	public Spell getSubSpell() {
		return hitSpell != null ? hitSpell.getSpell() : null;
	}

	public BeamSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigVector("relative-offset", "0,0.5,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.5,0");

		hitRadius = getConfigDouble("hit-radius", 2);
		maxDistance = getConfigDouble("max-distance", 30);
		verticalHitRadius = getConfigDouble("vertical-hit-radius", 2);

		yOffset = getConfigFloat("y-offset", 0F);
		gravity = getConfigFloat("gravity", 0F);
		interval = getConfigFloat("interval", 1F);
		rotation = getConfigFloat("rotation", 0F);
		beamVertOffset = getConfigFloat("beam-vert-offset", 0F);
		beamHorizOffset = getConfigFloat("beam-horiz-offset", 0F);

		changePitch = getConfigBoolean("change-pitch", true);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);

		hitSpellName = getConfigString("spell", "");
		endSpellName = getConfigString("spell-on-end", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");

		gravity *= -1;
		if (interval < 0.01) interval = 0.01F;
		if (yOffset != 0) relativeOffset.setY(yOffset);
	}

	@Override
	public void initialize() {
		super.initialize();

		hitSpell = new Subspell(hitSpellName);
		if (!hitSpell.process()) {
			if (!hitSpellName.isEmpty()) MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell defined!");
			hitSpell = null;
		}

		endSpell = new Subspell(endSpellName);
		if (!endSpell.process() || !endSpell.isTargetedLocationSpell()) {
			if (!endSpellName.isEmpty()) MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell-on-end defined!");
			endSpell = null;
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			if (!groundSpellName.isEmpty()) MagicSpells.error("BeamSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			groundSpell = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			new Beam(livingEntity, livingEntity.getLocation(), power);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity livingEntity, LivingEntity target, float power) {
		new Beam(livingEntity, livingEntity.getLocation(), target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location location, float v) {
		new Beam(livingEntity, location, v);
		return true;
	}

	@Override
	public boolean castAtLocation(Location location, float v) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		new Beam(caster, from, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return false;
	}

	private class Beam {

		private LivingEntity caster;
		private LivingEntity target;
		private float power;
		private Location startLoc;
		private Location currentLoc;
		private Set<Entity> immune;

		private Beam(LivingEntity caster, Location from, float power) {
			this.caster = caster;
			this.power = power;
			startLoc = from.clone();
			if (!changePitch) startLoc.setPitch(0F);
			immune = new HashSet<>();

			shootBeam();
		}

		private Beam(LivingEntity caster, Location from, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			startLoc = from.clone();
			if (!changePitch) startLoc.setPitch(0F);
			immune = new HashSet<>();

			shootBeam();
		}

		private void shootBeam() {
			playSpellEffects(EffectPosition.CASTER, caster);

			if (beamVertOffset != 0) startLoc.setPitch(startLoc.getPitch() - beamVertOffset);
			if (beamHorizOffset != 0) startLoc.setYaw(startLoc.getYaw() + beamHorizOffset);

			Vector startDir;
			if (target == null) startDir = startLoc.getDirection().normalize();
			else startDir = target.getLocation().toVector().subtract(startLoc.clone().toVector()).normalize();

			//apply relative offset
			Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
			startLoc.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			startLoc.add(startLoc.getDirection().clone().multiply(relativeOffset.getX()));
			startLoc.setY(startLoc.getY() + relativeOffset.getY());

			currentLoc = startLoc.clone();

			//apply target relative offset
			Location targetLoc = null;
			if (target != null) {
				targetLoc = target.getLocation().clone();
				startDir = targetLoc.clone().getDirection().normalize();
				horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();
				targetLoc.add(horizOffset.multiply(targetRelativeOffset.getZ())).getBlock().getLocation();
				targetLoc.add(targetLoc.getDirection().multiply(targetRelativeOffset.getX()));
				targetLoc.setY(target.getLocation().getY() + targetRelativeOffset.getY());
			}

			Vector dir;
			if (target == null) dir = startLoc.getDirection().multiply(interval);
			else dir = targetLoc.toVector().subtract(startLoc.clone().toVector()).normalize().multiply(interval);

			BoundingBox box = new BoundingBox(currentLoc, hitRadius, verticalHitRadius);

			float d = 0;
			mainLoop:
			while (d < maxDistance) {

				d += interval;
				currentLoc.add(dir);

				if (rotation != 0) Util.rotateVector(dir, rotation);
				if (gravity != 0) dir.add(new Vector(0, gravity,0));
				if (rotation != 0 || gravity != 0) currentLoc.setDirection(dir);

				//check block collision
				if (!isTransparent(currentLoc.getBlock())) {
					playSpellEffects(EffectPosition.DISABLED, currentLoc);
					if (groundSpell != null) groundSpell.castAtLocation(caster, currentLoc, power);
					if (stopOnHitGround) break;
				}

				playSpellEffects(EffectPosition.SPECIAL, currentLoc);

				box.setCenter(currentLoc);

				//check entities in the beam range
				for (LivingEntity e : startLoc.getWorld().getLivingEntities()) {
					if (e.equals(caster)) continue;
					if (e.isDead()) continue;
					if (immune.contains(e)) continue;
					if (!box.contains(e)) continue;
					if (validTargetList != null && !validTargetList.canTarget(e)) continue;

					SpellTargetEvent event = new SpellTargetEvent(BeamSpell.this, caster, e, power);
					EventUtil.call(event);
					if (event.isCancelled()) continue;
					LivingEntity entity = event.getTarget();

					if (hitSpell != null) {
						if (hitSpell.isTargetedEntitySpell()) hitSpell.castAtEntity(caster, entity, event.getPower());
						else if (hitSpell.isTargetedLocationSpell()) hitSpell.castAtLocation(caster, entity.getLocation(), event.getPower());
					}

					playSpellEffects(EffectPosition.TARGET, entity);
					playSpellEffectsTrail(caster.getLocation(), entity.getLocation());
					immune.add(e);

					if (stopOnHitEntity) break mainLoop;
				}
			}

			//end of the beam
			if (d >= maxDistance) {
				playSpellEffects(EffectPosition.DELAYED, currentLoc);
				if (endSpell != null) endSpell.castAtLocation(caster, currentLoc, power);
			}
		}

	}

}
