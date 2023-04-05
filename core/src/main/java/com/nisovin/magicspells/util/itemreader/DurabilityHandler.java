package com.nisovin.magicspells.util.itemreader;

import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

public class DurabilityHandler {

	public static void process(ConfigurationSection config, ItemStack stack) {
		stack.setDurability((short)config.getInt("damage"));
	}

}
