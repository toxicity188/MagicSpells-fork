package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.*;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.util.EulerAngle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class BlockBeamSpell extends InstantSpell implements TargetedLocationSpell, TargetedEntitySpell, TargetedEntityFromLocationSpell, SingleSpellSupplier {

	private Set<List<LivingEntity>> listSet;

	private Material material;
	private String materialName;

	private Vector relativeOffset;
	private Vector targetRelativeOffset;

	private int removeDelay;

	private double health;
	private double hitRadius;
	private double maxDistance;
	private double verticalHitRadius;

	private float gravity;
	private float yOffset;
	private float interval;
	private float rotation;
	private float rotationX;
	private float rotationY;
	private float rotationZ;
	private float beamVertOffset;
	private float beamHorizOffset;

	private boolean small;
	private boolean hpFix;
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

	public BlockBeamSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		listSet = new HashSet<>();

		materialName = getConfigString("block-type", "stone").toUpperCase();
		material = Material.getMaterial(materialName);

		relativeOffset = getConfigVector("relative-offset", "0,0.5,0");
		targetRelativeOffset = getConfigVector("target-relative-offset", "0,0.5,0");

		removeDelay = getConfigInt("remove-delay", 40);

		health = getConfigDouble("health", 2000);
		hitRadius = getConfigDouble("hit-radius", 2);
		maxDistance = getConfigDouble("max-distance", 30);
		verticalHitRadius = getConfigDouble("vertical-hit-radius", 2);

		gravity = getConfigFloat("gravity", 0F);
		yOffset = getConfigFloat("y-offset", 0F);
		interval = getConfigFloat("interval", 1F);
		rotation = getConfigFloat("rotation", 0F);
		rotationX = getConfigFloat("rotation-x", 0F);
		rotationY = getConfigFloat("rotation-y", 0F);
		rotationZ = getConfigFloat("rotation-z", 0F);
		beamVertOffset = getConfigFloat("beam-vert-offset", 0F);
		beamHorizOffset = getConfigFloat("beam-horiz-offset", 0F);

		small = getConfigBoolean("small", false);
		hpFix = getConfigBoolean("use-hp-fix", false);
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

		if (material == null || !material.isBlock()) {
			MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid block-type defined!");
			material = null;
		}

		hitSpell = new Subspell(hitSpellName);
		if (!hitSpell.process()) {
			if (!hitSpellName.isEmpty()) MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid spell defined!");
			hitSpell = null;
		}

		endSpell = new Subspell(endSpellName);
		if (!endSpell.process() || !endSpell.isTargetedLocationSpell()) {
			if (!endSpellName.isEmpty()) MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid spell-on-end defined!");
			endSpell = null;
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			if (!groundSpellName.isEmpty()) MagicSpells.error("BlockBeamSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			groundSpell = null;
		}
	}

	@Override
	public void turnOff() {
		for (List<LivingEntity> entityList : listSet) {
			for (LivingEntity entity : entityList) {
				entity.remove();
			}
		}
		listSet.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			new BlockBeam(livingEntity, livingEntity.getLocation(), power);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		new BlockBeam(caster, caster.getLocation(), target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		return false;
	}

	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location location, Power v) {
		new BlockBeam(livingEntity, location, v);
		return true;
	}

	@Override
	public boolean castAtLocation(Location location, Power v) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, Power power) {
		new BlockBeam(caster, from, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, Power power) {
		return false;
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent e) {
		LivingEntity target = e.getTarget();
		if (target.hasMetadata("MSBlockBeam")) e.setCancelled(true);
	}

	private class BlockBeam {

		private LivingEntity caster;
		private LivingEntity target;
		private Power power;
		private Location startLoc;
		private Location currentLoc;
		private Set<Entity> immune;
		private List<LivingEntity> armorStandList;
		private ItemStack helmet;

		private BlockBeam(LivingEntity caster, Location from, Power power) {
			this.caster = caster;
			this.power = power;
			helmet = new ItemStack(material);
			startLoc = from.clone();
			if (!changePitch) startLoc.setPitch(0F);

			immune = new HashSet<>();
			armorStandList = new ArrayList<>();

			shootBeam();
		}

		private BlockBeam(LivingEntity caster, Location from, LivingEntity target, Power power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			helmet = new ItemStack(material);
			startLoc = from.clone();
			if (!changePitch) startLoc.setPitch(0F);

			immune = new HashSet<>();
			armorStandList = new ArrayList<>();

			shootBeam();
		}

		private void shootBeam() {
			if (helmet == null) return;
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
				currentLoc.setDirection(dir);

				//check block collision
				if (!isTransparent(currentLoc.getBlock())) {
					playSpellEffects(EffectPosition.DISABLED, currentLoc);
					if (groundSpell != null) groundSpell.castAtLocation(caster, currentLoc, power);
					if (stopOnHitGround) break;
				}

				double pitch = currentLoc.getPitch() * Math.PI / 180;

				ArmorStand armorStand;
				if (!small) armorStand = currentLoc.getWorld().spawn(currentLoc.clone().subtract(0, 1.7, 0), ArmorStand.class);
				else armorStand = currentLoc.getWorld().spawn(currentLoc.clone().subtract(0, 0.9, 0), ArmorStand.class);

				armorStand.setHelmet(helmet);
				armorStand.setGravity(false);
				armorStand.setVisible(false);
				armorStand.setCollidable(false);
				armorStand.setInvulnerable(true);
				armorStand.setRemoveWhenFarAway(true);
				armorStand.setHeadPose(new EulerAngle(pitch + rotationX, rotationY, rotationZ));
				armorStand.setMetadata("MSBlockBeam", new FixedMetadataValue(MagicSpells.getInstance(), "MSBlockBeam"));

				if (hpFix) {
					armorStand.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
					armorStand.setHealth(health);
				}
				if (small) armorStand.setSmall(small);
				armorStandList.add(armorStand);

				playSpellEffects(EffectPosition.SPECIAL, currentLoc);

				box.setCenter(currentLoc);

				//check entities in the beam range
				for (LivingEntity e : startLoc.getWorld().getLivingEntities()) {
					if (e.equals(caster)) continue;
					if (e.isDead()) continue;
					if (immune.contains(e)) continue;
					if (!box.contains(e)) continue;
					if (validTargetList != null && !validTargetList.canTarget(e)) continue;

					SpellTargetEvent event = new SpellTargetEvent(BlockBeamSpell.this, caster, e, power);
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

			listSet.add(armorStandList);

			MagicSpells.scheduleDelayedTask(() -> {
				for (LivingEntity entity : armorStandList) {
					entity.remove();
				}
				listSet.remove(armorStandList);
			}, removeDelay);
		}

	}

}
