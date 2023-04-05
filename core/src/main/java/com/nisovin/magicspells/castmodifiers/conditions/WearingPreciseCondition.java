package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.castmodifiers.Condition;

public class WearingPreciseCondition extends Condition {
	
	private ItemStack itemStack = null;
	
	@Override
	public boolean setVar(String var) {
		var = var.trim();
		ItemStack item = Util.predefinedItems.get(var.trim());
		if (InventoryUtil.isNothing(item)) return false;
		itemStack = item.clone();
		return true;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		EntityEquipment equip = target.getEquipment();
		if (equip != null) {
			if (check(equip.getHelmet())) return true;
			if (check(equip.getChestplate())) return true;
			if (check(equip.getLeggings())) return true;
			return check(equip.getBoots());
		}
		return false;
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}
	
	private boolean check(ItemStack item) {
		return itemStack.isSimilar(item);
	}
	
}
