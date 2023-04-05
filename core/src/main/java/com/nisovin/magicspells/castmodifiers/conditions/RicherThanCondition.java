package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

/**
 * Condition check to see if a player has more money than the target
 * 
 * @author TheComputerGeek2
 */
public class RicherThanCondition extends Condition {
	
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
		if (!(target instanceof Player)) return true;
		return MagicSpells.getMoneyHandler().checkMoney((Player) target) > MagicSpells.getMoneyHandler().checkMoney((Player) target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

}
