package com.nisovin.magicspells.util;

import com.nisovin.magicspells.power.Power;
import org.bukkit.entity.Entity;

public class TargetInfo<E extends Entity> {

	private E target;
	private Power power;
	
	public TargetInfo(E target, Power power) {
		this.target = target;
		this.power = power;
	}
	
	public E getTarget() {
		return this.target;
	}
	
	public Power getPower() {
		return this.power;
	}
	
}
