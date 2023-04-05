package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class SneakingCondition extends Condition {
	
	@Override
	public boolean setVar(String var) {
		return true;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (!(target instanceof Player)) return false;
		return ((Player) target).isSneaking();
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}
	
}
