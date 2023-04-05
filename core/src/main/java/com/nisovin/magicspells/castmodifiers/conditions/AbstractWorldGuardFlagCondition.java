package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import org.codemc.worldguardwrapper.region.IWrappedRegion;

public abstract class AbstractWorldGuardFlagCondition extends AbstractWorldGuardCondition {
	
	@Override
	public boolean setVar(String var) {
		if (!worldGuardEnabled()) return false;
		return parseVar(var);
	}
	
	protected abstract boolean parseVar(String var);

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(livingEntity, target.getLocation());
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		if (!(livingEntity instanceof Player)) return false;
		IWrappedRegion region = getTopPriorityRegion(location);
		return check(region, (Player) livingEntity);
	}
	
	protected abstract boolean check(IWrappedRegion region, Player player);

}
