package com.nisovin.magicspells.mana;

import org.bukkit.ChatColor;

public class ManaRank {
	
	String name;
	int startingMana;
	int maxMana;
	int regenAmount;
	String prefix;
	ChatColor colorFull;
	ChatColor colorEmpty;
	
	@Override
	public String toString() {
		return "ManaRank:["
			+ "name=" + name
			+ ",startingMana=" + startingMana
			+ ",maxMana=" + maxMana
			+ ",regenAmount=" + regenAmount
			+ ",prefix=" + prefix
			+ ",colorFull=" + colorFull
			+ ",colorEmpty=" + colorEmpty
			+ ']';
	}
	
}
