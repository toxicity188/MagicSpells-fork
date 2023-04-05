package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import org.codemc.worldguardwrapper.WorldGuardWrapper;

public class InRegionCondition extends Condition {

	private final WorldGuardWrapper worldGuard = WorldGuardWrapper.getInstance();
	private String worldName;
	private String regionName;
	
	@Override
	public boolean setVar(String var) {
		if (var == null) return false;

		if (worldGuard.getWorldGuardPlugin() == null || !worldGuard.getWorldGuardPlugin().isEnabled()) return false;
		
		String[] split = var.split(":");
		if (split.length == 2) {
			worldName = split[0];
			regionName = split[1];
			return true;
		}
		return false;
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
		World world = Bukkit.getWorld(worldName);

		if (world == null) return false;
		if (!world.equals(location.getWorld())) return false;

		return worldGuard.getRegions(world).values().stream().anyMatch(r -> r.contains(location));
	}

}
