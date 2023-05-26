package com.nisovin.magicspells.spells;

import com.nisovin.magicspells.power.Power;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface TargetedLocationSpell {
	
	boolean castAtLocation(LivingEntity caster, Location target, Power power);

	boolean castAtLocation(Location target, Power power);
	
}
