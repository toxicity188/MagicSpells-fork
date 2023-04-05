package com.nisovin.magicspells.util.projectile;

import org.bukkit.entity.Snowball;
import org.bukkit.entity.Projectile;

public class ProjectileManagerSnowball extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return Snowball.class;
	}
	
}
