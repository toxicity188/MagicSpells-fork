package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.SingleSpellSupplier;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class HoldRightSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell, SingleSpellSupplier {

	private int resetTime;

	private float maxDuration;
	private float maxDistance;

	private boolean targetEntity;
	private boolean targetLocation;

	private Subspell spellToCast;
	private String spellToCastName;

	@Override
	public Spell getSubSpell() {
		return spellToCast != null ? spellToCast.getSpell() : null;
	}

	private Map<UUID, CastData> casting;
	
	public HoldRightSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		resetTime = getConfigInt("reset-time", 250);

		maxDuration = getConfigFloat("max-duration", 0F);
		maxDistance = getConfigFloat("max-distance", 0F);

		targetEntity = getConfigBoolean("target-entity", true);
		targetLocation = getConfigBoolean("target-location", false);

		spellToCastName = getConfigString("spell", "");

		casting = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process()) {
			spellToCast = null;
			MagicSpells.error("HoldRightSpell '" + internalName + "' has an invalid spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			CastData data = casting.get(livingEntity.getUniqueId());
			if (data != null && data.isValid(livingEntity)) {
				data.cast(livingEntity);
				return PostCastAction.ALREADY_HANDLED;
			}

			if (targetEntity) {
				TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
				if (target != null) data = new CastData(target.getTarget(), target.getPower());
				else return noTarget(livingEntity);
			} else if (targetLocation) {
				Block block = getTargetedBlock(livingEntity, power);
				if (block != null && block.getType() != Material.AIR) data = new CastData(block.getLocation().add(0.5, 0.5, 0.5), power);
				else return noTarget(livingEntity);
			} else data = new CastData(power);

			if (data != null) {
				data.cast(livingEntity);
				casting.put(livingEntity.getUniqueId(), data);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		if (!targetLocation) return false;
		CastData data = casting.get(caster.getUniqueId());
		if (data != null && data.isValid(caster)) {
			data.cast(caster);
			return true;
		}
		data = new CastData(target, power);
		data.cast(caster);
		casting.put(caster.getUniqueId(), data);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		if (!targetEntity) return false;
		CastData data = casting.get(caster.getUniqueId());
		if (data != null && data.isValid(caster)) {
			data.cast(caster);
			return true;
		}
		data = new CastData(target, power);
		data.cast(caster);
		casting.put(caster.getUniqueId(), data);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		return false;
	}
	
	private class CastData {

		private Location targetLocation = null;
		private LivingEntity targetEntity = null;

		private Power power;

		private long start = System.currentTimeMillis();
		private long lastCast = 0;
		
		private CastData(LivingEntity target, Power power) {
			targetEntity = target;
			this.power = power;
		}

		private CastData(Location target, Power power) {
			targetLocation = target;
			this.power = power;
		}

		private CastData(Power power) {
			this.power = power;
		}

		private boolean isValid(LivingEntity livingEntity) {
			if (lastCast < System.currentTimeMillis() - resetTime) return false;
			if (maxDuration > 0 && System.currentTimeMillis() - start > maxDuration * TimeUtil.MILLISECONDS_PER_SECOND) return false;
			if (maxDistance > 0) {
				Location l = targetLocation;
				if (targetEntity != null) l = targetEntity.getLocation();
				if (l == null) return false;
				if (!l.getWorld().equals(livingEntity.getWorld())) return false;
				if (l.distanceSquared(livingEntity.getLocation()) > maxDistance * maxDistance) return false;
			}
			return true;
		}

		private void cast(LivingEntity caster) {
			lastCast = System.currentTimeMillis();
			if (targetEntity != null) spellToCast.castAtEntity(caster, targetEntity, power);
			else if (targetLocation != null) spellToCast.castAtLocation(caster, targetLocation, power);
			else spellToCast.cast(caster, power);
		}
		
	}

}
