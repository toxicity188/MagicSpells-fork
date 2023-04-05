package com.nisovin.magicspells.util;

import org.bukkit.entity.LivingEntity;

@FunctionalInterface
public interface ValidTargetChecker {

	boolean isValidTarget(LivingEntity entity);

}
