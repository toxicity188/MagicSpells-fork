package com.nisovin.magicspells.spelleffects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;

public class ItemCooldownEffect extends SpellEffect {

	private ItemStack item;

	private int duration;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		item = Util.getItemStackFromString(config.getString("item", "stone"));
		duration = config.getInt("duration", TimeUtil.TICKS_PER_SECOND);
	}
	
	@Override
	protected Runnable playEffectEntity(Entity entity) {
		if (!(entity instanceof Player)) return null;
		MagicSpells.getVolatileCodeHandler().showItemCooldown((Player) entity, item, duration);
		return null;
	}
	
}
