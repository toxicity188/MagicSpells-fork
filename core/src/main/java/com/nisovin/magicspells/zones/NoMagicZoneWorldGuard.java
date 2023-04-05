package com.nisovin.magicspells.zones;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;

public class NoMagicZoneWorldGuard extends NoMagicZone {

	private String worldName;
	private String regionName;
	private IWrappedRegion region;

	@Override
	public void initialize(ConfigurationSection config) {
		worldName = config.getString("world", "");
		regionName = config.getString("region", "");
	}

	@Override
	public boolean inZone(Location location) {
		// Check world
		if (!worldName.equals(location.getWorld().getName())) return false;

		// Get region, if necessary
		if (region == null) {
			WorldGuardWrapper wrapper = WorldGuardWrapper.getInstance();
			if (wrapper.getWorldGuardPlugin() != null && wrapper.getWorldGuardPlugin().isEnabled()) {

				World w = Bukkit.getServer().getWorld(worldName);
				if (w != null) {
					region = wrapper.getRegions(w).get(regionName);
				}
			}
		}
		// Check if contains
		if (region != null) {
			return region.contains(location);
		}

		MagicSpells.error("Failed to access WorldGuard region '" + regionName + '\'');
		return false;
	}

}
