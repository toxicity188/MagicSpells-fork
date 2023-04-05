package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class InWorldCondition extends Condition {

	private String world = "";

	@Override
	public boolean setVar(String var) {
		world = var;
		return true;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target, target.getLocation());
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return location.getWorld().getName().equalsIgnoreCase(world);
	}

}
