package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface TargetedLocationSpell {
	
	boolean castAtLocation(LivingEntity caster, Location target, float power);

	boolean castAtLocation(Location target, float power);
	
}
