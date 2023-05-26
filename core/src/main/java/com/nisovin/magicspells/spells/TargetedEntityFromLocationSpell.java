package com.nisovin.magicspells.spells;

import com.nisovin.magicspells.power.Power;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface TargetedEntityFromLocationSpell {
	
	boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, Power power);
	
	boolean castAtEntityFromLocation(Location from, LivingEntity target, Power power);

}
