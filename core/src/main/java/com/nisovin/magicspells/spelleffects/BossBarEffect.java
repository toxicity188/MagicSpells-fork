package com.nisovin.magicspells.spelleffects;

import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.Variable;

public class BossBarEffect extends SpellEffect {

	private String title;
	private String color;
	private String style;

	private String strVar;
	private Variable variable;
	private double maxValue;

	private BarColor barColor;
	private BarStyle barStyle;

	private int duration;
	private double progress;

	private boolean broadcast;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		title = ChatColor.translateAlternateColorCodes('&', config.getString("title", ""));
		color = config.getString("color", "red").toUpperCase();
		style = config.getString("style", "solid").toUpperCase();
		strVar = config.getString("variable", "");
		maxValue = config.getDouble("max-value", 100);

		variable = MagicSpells.getVariableManager().getVariable(strVar);
		if (variable == null && !strVar.isEmpty()) {
			MagicSpells.error("Wrong variable defined! '" + strVar + "'");
		}

		barColor = BarColor.valueOf(color);
		if (barColor == null) {
			MagicSpells.error("Wrong bar color defined! '" + color + "'");
		}

		barStyle = BarStyle.valueOf(style);
		if (barStyle == null) {
			MagicSpells.error("Wrong bar style defined! '" + style + "'");
		}

		duration = config.getInt("duration", 60);
		progress = config.getDouble("progress", 1);
		if (progress > 1) progress = 1;
		if (progress < 0) progress = 0;

		broadcast = config.getBoolean("broadcast", false);
	}

	@Override
	protected Runnable playEffectEntity(Entity entity) {
		if (barStyle == null || barColor == null) return null;
		if (broadcast) Util.forEachPlayerOnline(this::createBar);
		else if (entity instanceof Player) createBar((Player) entity);
		return null;
	}

	private void createBar(Player player) {
		if (variable != null) createVariableBar(player);
		else MagicSpells.getBossBarManager().setPlayerBar(player, title, progress, barStyle, barColor);
		MagicSpells.scheduleDelayedTask(() -> MagicSpells.getBossBarManager().removePlayerBar(player), duration);
	}

	private void createVariableBar(Player player) {
		double diff = variable.getValue(player) / maxValue;
		if (diff > 1 || diff < 0) return;
		MagicSpells.getBossBarManager().setPlayerBar(player, title, diff, barStyle, barColor);
	}

}
