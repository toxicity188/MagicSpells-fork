package com.nisovin.magicspells.zones;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.Spell;

public abstract class NoMagicZone implements Comparable<NoMagicZone> {

	private String id;
	
	private int priority;
	
	private String message;
	
	private List<String> allowedSpells;
	
	private List<String> disallowedSpells;
	
	private boolean allowAll;
	
	private boolean disallowAll;
	
	public final void create(String id, ConfigurationSection config) {
		this.id = id;
		priority = config.getInt("priority", 0);
		message = config.getString("message", "You are in a no-magic zone.");
		allowedSpells = config.getStringList("allowed-spells");
		disallowedSpells = config.getStringList("disallowed-spells");
		allowAll = config.getBoolean("allow-all", false);
		disallowAll = config.getBoolean("disallow-all", true);
		if (allowedSpells != null && allowedSpells.isEmpty()) allowedSpells = null;
		if (disallowedSpells != null && disallowedSpells.isEmpty()) disallowedSpells = null;
		if (disallowedSpells != null) disallowAll = false;
		if (allowedSpells != null) allowAll = false;
		initialize(config);
	}
	
	public abstract void initialize(ConfigurationSection config);
	
	public final ZoneCheckResult check(Player player, Spell spell) {
		return check(player.getLocation(), spell);
	}
	
	public final ZoneCheckResult check(Location location, Spell spell) {
		if (!inZone(location)) return ZoneCheckResult.IGNORED;
		if (disallowedSpells != null && disallowedSpells.contains(spell.getInternalName())) return ZoneCheckResult.DENY;
		if (allowedSpells != null && allowedSpells.contains(spell.getInternalName())) return ZoneCheckResult.ALLOW;
		if (disallowAll) return ZoneCheckResult.DENY;
		if (allowAll) return ZoneCheckResult.ALLOW;
		return ZoneCheckResult.IGNORED;
	}
	
	public abstract boolean inZone(Location location);
	
	public String getId() {
		return id;
	}
	
	public String getMessage() {
		return message;
	}
	
	@Override
	public int compareTo(NoMagicZone other) {
		if (priority < other.priority) return 1;
		if (priority > other.priority) return -1;
		return id.compareTo(other.id);
	}
	
	public enum ZoneCheckResult {
		
		ALLOW,
		DENY,
		IGNORED
		
	}
	
}
