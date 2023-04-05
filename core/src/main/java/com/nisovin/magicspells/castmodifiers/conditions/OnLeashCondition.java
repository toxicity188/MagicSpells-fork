package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnLeashCondition extends Condition {

	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return livingEntity.isLeashed();
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return target.isLeashed();
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
