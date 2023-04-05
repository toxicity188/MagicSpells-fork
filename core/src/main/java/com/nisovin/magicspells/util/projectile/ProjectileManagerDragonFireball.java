package com.nisovin.magicspells.util.projectile;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.DragonFireball;

public class ProjectileManagerDragonFireball extends ProjectileManager {
	
	@Override
	public Class<? extends Projectile> getProjectileClass() {
		return DragonFireball.class;
	}
	
}
