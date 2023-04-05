package com.nisovin.magicspells.spells;

import org.bukkit.entity.LivingEntity;

public interface TargetedEntitySpell {
	
	boolean castAtEntity(LivingEntity caster, LivingEntity target, float power);
	
	boolean castAtEntity(LivingEntity target, float power);
	
}
