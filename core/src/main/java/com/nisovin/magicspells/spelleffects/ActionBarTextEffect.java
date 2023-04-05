package com.nisovin.magicspells.spelleffects;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;

public class ActionBarTextEffect extends SpellEffect {

	private String message;

	private boolean broadcast;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		message = ChatColor.translateAlternateColorCodes('&', config.getString("message", ""));
		broadcast = config.getBoolean("broadcast", false);
	}
	
	@Override
	protected Runnable playEffectEntity(Entity entity) {
		if (broadcast) Util.forEachPlayerOnline(this::send);
		else if (entity instanceof Player) send((Player) entity);
		return null;
	}
	
	private void send(Player player) {
		MagicSpells.getVolatileCodeHandler().sendActionBarMessage(player, MagicSpells.doVariableReplacements(player, message));
	}
	
}
