package com.nisovin.magicspells.util;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;

public interface IBossBarManager {

	void setPlayerBar(Player player, String title, double progress, BarStyle style, BarColor color);

	void setPlayerBar(Player player, String title, double progress, BarStyle style);

	void setPlayerBar(Player player, String title, double progress);

	void addPlayerBarFlag(Player player, BarFlag flag);

	void removePlayerBar(Player player);
	
	void turnOff();
	
}
