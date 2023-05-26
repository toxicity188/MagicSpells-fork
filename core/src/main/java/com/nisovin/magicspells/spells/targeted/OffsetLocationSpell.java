package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class OffsetLocationSpell extends TargetedSpell implements TargetedLocationSpell {

	private Vector relativeOffset;
	private Vector absoluteOffset;
	
	private Subspell spellToCast;
	private String spellToCastName;
	
	public OffsetLocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		absoluteOffset = getConfigVector("absolute-offset", "0,0,0");
		
		spellToCastName = getConfigString("spell", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process() || !spellToCast.isTargetedLocationSpell()) {
			MagicSpells.error("OffsetLocationSpell '" + internalName + "' has an invalid spell defined!");
			spellToCast = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location baseTargetLocation;
			TargetInfo<LivingEntity> entityTargetInfo = getTargetedEntity(livingEntity, power);
			if (entityTargetInfo != null && entityTargetInfo.getTarget() != null) baseTargetLocation = entityTargetInfo.getTarget().getLocation();
			else baseTargetLocation = getTargetedBlock(livingEntity, power).getLocation();
			if (baseTargetLocation == null) return noTarget(livingEntity);
			Location loc = Util.applyOffsets(baseTargetLocation, relativeOffset, absoluteOffset);
			if (loc == null) return PostCastAction.ALREADY_HANDLED;

			if (spellToCast != null) spellToCast.castAtLocation(livingEntity, loc, power);
			playSpellEffects(livingEntity, loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		if (spellToCast != null) spellToCast.castAtLocation(caster, Util.applyOffsets(target, relativeOffset, absoluteOffset), power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		return castAtLocation(null, target, power);
	}
	
}
