package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class OrbitSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private ValidTargetList entityTargetList;
	private List<String> targetList;

	private double maxDuration;

	private int tickInterval;
	private int vertExpandDelay;
	private int horizExpandDelay;

	private float yOffset;
	private float hitRadius;
	private float orbitRadius;
	private float horizOffset;
	private float ticksPerSecond;
	private float distancePerTick;
	private float vertExpandRadius;
	private float verticalHitRadius;
	private float horizExpandRadius;
	private float secondsPerRevolution;

	private boolean stopOnHitEntity;
	private boolean stopOnHitGround;
	private boolean counterClockwise;
	private boolean requireEntityTarget;

	private String orbitSpellName;
	private String groundSpellName;
	private String entitySpellName;

	private Subspell orbitSpell;
	private Subspell groundSpell;
	private Subspell entitySpell;

	public OrbitSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targetList = getConfigStringList("can-hit", null);
		entityTargetList = new ValidTargetList(this, targetList);

		maxDuration = getConfigDouble("max-duration", 20) * (double) TimeUtil.MILLISECONDS_PER_SECOND;

		tickInterval = getConfigInt("tick-interval", 2);
		vertExpandDelay = getConfigInt("vert-expand-delay", 0);
		horizExpandDelay = getConfigInt("horiz-expand-delay", 0);

		yOffset = getConfigFloat("y-offset", 0.6F);
		hitRadius = getConfigFloat("hit-radius", 1F);
		orbitRadius = getConfigFloat("orbit-radius", 1F);
		horizOffset = getConfigFloat("start-horiz-offset", 0);
		vertExpandRadius = getConfigFloat("vert-expand-radius", 0);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", 1F);
		horizExpandRadius = getConfigFloat("horiz-expand-radius", 0);
		secondsPerRevolution = getConfigFloat("seconds-per-revolution", 3F);

		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", false);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", false);
		counterClockwise = getConfigBoolean("counter-clockwise", false);
		requireEntityTarget = getConfigBoolean("require-entity-target", true);

		orbitSpellName = getConfigString("spell", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		entitySpellName = getConfigString("spell-on-hit-entity", "");

		ticksPerSecond = 20F / (float) tickInterval;
		distancePerTick = 6.28F / (ticksPerSecond * secondsPerRevolution);
	}

	@Override
	public void initialize() {
		super.initialize();

		orbitSpell = new Subspell(orbitSpellName);
		if (!orbitSpell.process() || !orbitSpell.isTargetedLocationSpell()) {
			orbitSpell = null;
			if (!orbitSpellName.isEmpty()) MagicSpells.error("OrbitSpell '" + internalName + "' has an invalid spell defined!");
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			groundSpell = null;
			if (!groundSpellName.isEmpty()) MagicSpells.error("OrbitSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
		}

		entitySpell = new Subspell(entitySpellName);
		if (!entitySpell.process() || !entitySpell.isTargetedEntitySpell()) {
			entitySpell = null;
			if (!entitySpellName.isEmpty()) MagicSpells.error("OrbitSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
				if (target == null) return noTarget(livingEntity);
				new OrbitTracker(livingEntity, target.getTarget(), target.getPower());
				playSpellEffects(livingEntity, target.getTarget());
				sendMessages(livingEntity, target.getTarget());
				return PostCastAction.NO_MESSAGES;
			}

			Block block = getTargetedBlock(livingEntity, power);
			if (block != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, livingEntity, block.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) block = null;
				else {
					block = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (block == null) return noTarget(livingEntity);

			new OrbitTracker(livingEntity, block.getLocation().add(0.5, 0, 0.5), power);
			return PostCastAction.HANDLE_NORMALLY;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		new OrbitTracker(caster, target, power);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		new OrbitTracker(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private class OrbitTracker implements Runnable {

		private LivingEntity caster;
		private LivingEntity target;
		private Location targetLoc;
		private Vector currentPosition;
		private BoundingBox box;
		private Set<LivingEntity> immune;

		private float power;
		private float orbRadius;
		private float orbHeight;

		private int taskId;
		private int repeatingHorizTaskId;
		private int repeatingVertTaskId;

		private long startTime;

		private OrbitTracker(LivingEntity caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;

			targetLoc = target.getLocation();
			initialize();
		}

		private OrbitTracker(LivingEntity caster, Location targetLoc, float power) {
			this.caster = caster;
			this.targetLoc = targetLoc;
			this.power = power;

			initialize();
		}

		private void initialize() {
			startTime = System.currentTimeMillis();
			currentPosition = targetLoc.getDirection().setY(0);
			Util.rotateVector(currentPosition, horizOffset);
			taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			orbRadius = orbitRadius;
			orbHeight = yOffset;
			immune = new HashSet<>();

			box = new BoundingBox(targetLoc, hitRadius, verticalHitRadius);

			if (horizExpandDelay > 0) repeatingHorizTaskId = MagicSpells.scheduleRepeatingTask(() -> orbRadius += horizExpandRadius, horizExpandDelay, horizExpandDelay);
			if (vertExpandDelay > 0) repeatingVertTaskId = MagicSpells.scheduleRepeatingTask(() -> orbHeight += vertExpandRadius, vertExpandDelay, vertExpandDelay);
		}

		@Override
		public void run() {
			if (!caster.isValid() || (target != null && !target.isValid())) {
				stop();
				return;
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				stop();
				return;
			}

			if (target != null) targetLoc = target.getLocation();

			Location loc = getLocation();

			if (!isTransparent(loc.getBlock())) {
				if (groundSpell != null) groundSpell.castAtLocation(caster, loc, power);
				if (stopOnHitGround) {
					stop();
					return;
				}
			}

			playSpellEffects(EffectPosition.SPECIAL, loc);

			if (orbitSpell != null) orbitSpell.castAtLocation(caster, loc, power);

			box.setCenter(loc);

			for (LivingEntity e : caster.getWorld().getLivingEntities()) {
				if (e.equals(caster)) continue;
				if (e.isDead()) continue;
				if (immune.contains(e)) continue;
				if (!box.contains(e)) continue;
				if (entityTargetList != null && !entityTargetList.canTarget(e)) continue;

				SpellTargetEvent event = new SpellTargetEvent(OrbitSpell.this, caster, e, power);
				EventUtil.call(event);
				if (event.isCancelled()) continue;

				immune.add(event.getTarget());
				if (entitySpell != null) entitySpell.castAtEntity(event.getCaster(), event.getTarget(), event.getPower());
				playSpellEffects(EffectPosition.TARGET, event.getTarget());
				playSpellEffectsTrail(targetLoc, event.getTarget().getLocation());
				if (stopOnHitEntity) {
					stop();
					return;
				}
			}
		}

		private Location getLocation() {
			Vector perp;
			if (counterClockwise) perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
			else perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
			currentPosition.add(perp.multiply(distancePerTick)).normalize();
			return targetLoc.clone().add(0, orbHeight, 0).add(currentPosition.clone().multiply(orbRadius));
		}


		private void stop() {
			if (target != null && target.isValid()) playSpellEffects(EffectPosition.DELAYED, getLocation());
			MagicSpells.cancelTask(taskId);
			MagicSpells.cancelTask(repeatingHorizTaskId);
			MagicSpells.cancelTask(repeatingVertTaskId);
			caster = null;
			target = null;
			targetLoc = null;
			currentPosition = null;
		}

	}

}
