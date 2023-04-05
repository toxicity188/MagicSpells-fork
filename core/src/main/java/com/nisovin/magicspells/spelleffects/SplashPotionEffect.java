package com.nisovin.magicspells.spelleffects;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class SplashPotionEffect extends SpellEffect {

	private int pot;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		pot = config.getInt("potion", 0);
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		location.getWorld().playEffect(location, Effect.POTION_BREAK, pot);
		return null;
	}
	
}
