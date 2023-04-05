package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class CustomNameCondition extends Condition {

	private String name;
	private boolean isVar;

	@Override
	public boolean setVar(String var) {
		if (var == null || var.isEmpty()) return false;
		name = ChatColor.translateAlternateColorCodes('&', var);
		if (name.contains("%var:")) isVar = true;
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return checkName(livingEntity, target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean checkName(LivingEntity livingEntity, LivingEntity target) {
		if (!(livingEntity instanceof Player)) return false;
		if (isVar) name = MagicSpells.doVariableReplacements((Player) livingEntity, name);
		String n = target.getCustomName();
		if (!isVar) name = name.replace("__", " ");
		return n != null && !n.isEmpty() && name.equalsIgnoreCase(n);
	}

}
