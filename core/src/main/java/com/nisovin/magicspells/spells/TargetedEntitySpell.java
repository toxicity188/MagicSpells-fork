package com.nisovin.magicspells.spells;

import com.nisovin.magicspells.power.Power;
import org.bukkit.entity.LivingEntity;

public interface TargetedEntitySpell {
	
	boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power);
	
	boolean castAtEntity(LivingEntity target, Power power);
	
}
