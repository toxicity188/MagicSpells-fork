package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell;
import com.nisovin.magicspells.spells.targeted.DisguiseSpell.Disguise;

public class DisguisedCondition extends Condition {

	private String disguiseName;
	
	@Override
	public boolean setVar(String var) {
		disguiseName = var;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		Disguise disguise = DisguiseSpell.getDisguiseManager().getDisguise((Player) livingEntity);
		if (disguise != null) {
			if (disguiseName == null || disguiseName.isEmpty()) return true;
			if (disguise.getSpell().getInternalName().equals(disguiseName)) return true;
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
