package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OffhandCondition extends Condition {

	private Material[] ids;
	private short[] datas;
	private boolean[] checkData;
	private String[] names;
	private boolean[] checkName;
	
	@Override
	public boolean setVar(String var) {
		try {
			String[] vardata = var.split(",");
			ids = new Material[vardata.length];
			datas = new short[vardata.length];
			checkData = new boolean[vardata.length];
			names = new String[vardata.length];
			checkName = new boolean[vardata.length];
			for (int i = 0; i < vardata.length; i++) {
				if (vardata[i].contains("|")) {
					String[] subvardata = vardata[i].split("\\|");
					vardata[i] = subvardata[0];
					names[i] = ChatColor.translateAlternateColorCodes('&', subvardata[1]).replace("__", " ");
					if (names[i].isEmpty()) names[i] = null;
					checkName[i] = true;
				} else {
					names[i] = null;
					checkName[i] = false;
				}
				if (vardata[i].contains(":")) {
					String[] subvardata = vardata[i].split(":");
					ids[i] = Material.getMaterial(subvardata[0]);
					if (subvardata[1].equals("*")) {
						datas[i] = 0;
						checkData[i] = false;
					} else {
						datas[i] = Short.parseShort(subvardata[1]);
						checkData[i] = true;
					}
				} else {
					ids[i] = Material.getMaterial(vardata[i]);
					datas[i] = 0;
					checkData[i] = false;
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		ItemStack item = livingEntity.getEquipment().getItemInOffHand();
		return check(item);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return check(target);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}
	
	private boolean check(ItemStack item) {
		if (item == null) return false;
		Material type = item.getType();
		short durability = item.getDurability();
		String name = null;
		try {
			if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) name = item.getItemMeta().getDisplayName();
		} catch (Exception e) {
			// no op
		}
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == type && (!checkData[i] || datas[i] == durability) && (!checkName[i] || Objects.equals(names[i], name))) {
				return true;
			}
		}
		return false;
	}

}
