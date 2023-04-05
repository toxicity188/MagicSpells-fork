package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.SpellSupplier;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

import org.apache.commons.math3.util.FastMath;

public class AreaEffectSpell extends TargetedSpell implements TargetedLocationSpell, SpellSupplier {

	private List<Subspell> spells;
	private List<String> spellNames;

	private int maxTargets;

	private double cone;
	private double vRadius;
	private double hRadius;

	private boolean pointBlank;
	private boolean failIfNoTargets;
	private boolean spellSourceInCenter;
	
	public AreaEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellNames = getConfigStringList("spells", null);

		maxTargets = getConfigInt("max-targets", 0);

		cone = getConfigDouble("cone", 0);
		vRadius = getConfigDouble("vertical-radius", 5);
		hRadius = getConfigDouble("horizontal-radius", 10);

		pointBlank = getConfigBoolean("point-blank", true);
		failIfNoTargets = getConfigBoolean("fail-if-no-targets", true);
		spellSourceInCenter = getConfigBoolean("spell-source-in-center", false);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		spells = new ArrayList<>();

		if (spellNames == null || spellNames.isEmpty()) {
			MagicSpells.error("AreaEffectSpell '" + internalName + "' has no spells defined!");
			return;
		}
		
		for (String spellName : spellNames) {
			Subspell spell = new Subspell(spellName);

			if (!spell.process()) {
				MagicSpells.error("AreaEffectSpell '" + internalName + "' attempted to use invalid spell '" + spellName + '\'');
				continue;
			}

			if (!spell.isTargetedLocationSpell() && !spell.isTargetedEntityFromLocationSpell() && !spell.isTargetedEntitySpell()) {
				MagicSpells.error("AreaEffectSpell '" + internalName + "' attempted to use non-targeted spell '" + spellName + '\'');
				continue;
			}

			spells.add(spell);
		}

		spellNames.clear();
		spellNames = null;
	}
	public List<Spell> getSubSpells() {
		return spells.stream().map(Subspell::getSpell).collect(Collectors.toList());
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = livingEntity.getLocation();
			else {
				try {
					Block block = getTargetedBlock(livingEntity, power);
					if (block != null && !BlockUtils.isAir(block.getType())) loc = block.getLocation().add(0.5, 0, 0.5);
				} catch (IllegalStateException e) {
					loc = null;
				}
			}

			if (loc == null) return noTarget(livingEntity);

			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, livingEntity, loc, power);
			EventUtil.call(event);
			if (event.isCancelled()) loc = null;
			else {
				loc = event.getTargetLocation();
				power = event.getPower();
			}

			if (loc == null) return noTarget(livingEntity);
			
			boolean done = doAoe(livingEntity, loc, power);
			
			if (!done && failIfNoTargets) return noTarget(livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return doAoe(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return doAoe(null, target, power);
	}
	
	private boolean doAoe(LivingEntity livingEntity, Location location, float basePower) {
		int count = 0;

		Vector vLoc = livingEntity != null ? livingEntity.getLocation().toVector() : location.toVector();
		Vector facing = livingEntity != null ? livingEntity.getLocation().getDirection() : location.getDirection();

		BoundingBox box = new BoundingBox(location, hRadius, vRadius);
		List<Entity> entities = new ArrayList<>(location.getWorld().getEntitiesByClasses(LivingEntity.class));
		Collections.shuffle(entities);

		for (Entity e : entities) {
			if (e == null) continue;
			if (!box.contains(e)) continue;
			if (pointBlank && cone > 0) {
				Vector dir = e.getLocation().toVector().subtract(vLoc);
				if (FastMath.toDegrees(FastMath.abs(dir.angle(facing))) > cone) continue;
			}

			LivingEntity target = (LivingEntity) e;
			float power = basePower;

			if (target.isDead()) continue;
			if (livingEntity == null && !validTargetList.canTarget(target)) continue;
			if (livingEntity != null && !validTargetList.canTarget(livingEntity, target)) continue;

			SpellTargetEvent event = new SpellTargetEvent(this, livingEntity, target, power);
			EventUtil.call(event);
			if (event.isCancelled()) continue;

			target = event.getTarget();
			power = event.getPower();

			for (Subspell spell : spells) {
				if (spellSourceInCenter && spell.isTargetedEntityFromLocationSpell()) spell.castAtEntityFromLocation(livingEntity, location, target, power);
				else if (livingEntity != null && spell.isTargetedEntityFromLocationSpell()) spell.castAtEntityFromLocation(livingEntity, livingEntity.getLocation(), target, power);
				else if (spell.isTargetedEntitySpell()) spell.castAtEntity(livingEntity, target, power);
				else if (spell.isTargetedLocationSpell()) spell.castAtLocation(livingEntity, target.getLocation(), power);
			}

			playSpellEffects(EffectPosition.TARGET, target);
			if (spellSourceInCenter) playSpellEffectsTrail(location, target.getLocation());
			else if (livingEntity != null) playSpellEffectsTrail(livingEntity.getLocation(), target.getLocation());

			count++;

			if (maxTargets > 0 && count >= maxTargets) break;
		}

		if (count > 0 || !failIfNoTargets) {
			playSpellEffects(EffectPosition.SPECIAL, location);
			if (livingEntity != null) playSpellEffects(EffectPosition.CASTER, livingEntity);
		}
		
		return count > 0;
	}
	
}
