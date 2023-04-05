package com.nisovin.magicspells.spells;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface TargetedEntityFromLocationSpell {
	
	boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power);
	
	boolean castAtEntityFromLocation(Location from, LivingEntity target, float power);

}
