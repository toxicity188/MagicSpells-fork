package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import java.util.regex.Pattern;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class GateSpell extends InstantSpell {

	private static final Pattern COORDINATE_PATTERN = Pattern.compile("^-?[0-9]+,[0-9]+,-?[0-9]+(,-?[0-9.]+,-?[0-9.]+)?$");

	private String world;
	private String coords;
	private String strGateFailed;

	public GateSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		world = getConfigString("world", "CURRENT");
		coords = getConfigString("coordinates", "SPAWN");
		strGateFailed = getConfigString("str-gate-failed", "Unable to teleport.");
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			World effectiveWorld;
			if (world.equals("CURRENT")) effectiveWorld = livingEntity.getWorld();
			else if (world.equals("DEFAULT")) effectiveWorld = Bukkit.getServer().getWorlds().get(0);
			else effectiveWorld = Bukkit.getServer().getWorld(world);

			if (effectiveWorld == null) {
				MagicSpells.error("GateSpell '" + internalName + "' has a non existent world defined!");
				sendMessage(strGateFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Get location
			Location location;
			coords = coords.replace(" ", "");
			if (RegexUtil.matches(COORDINATE_PATTERN, coords)) {
				String[] c = coords.split(",");
				int x = Integer.parseInt(c[0]);
				int y = Integer.parseInt(c[1]);
				int z = Integer.parseInt(c[2]);
				float yaw = 0;
				float pitch = 0;
				if (c.length > 3) {
					yaw = Float.parseFloat(c[3]);
					pitch = Float.parseFloat(c[4]);
				}
				location = new Location(effectiveWorld, x, y, z, yaw, pitch);
			} else if (coords.equals("SPAWN")) {
				location = effectiveWorld.getSpawnLocation();
				location = new Location(effectiveWorld, location.getX(), effectiveWorld.getHighestBlockYAt(location), location.getZ());
			} else if (coords.equals("EXACTSPAWN")) {
				location = effectiveWorld.getSpawnLocation();
			} else if (coords.equals("CURRENT")) {
				Location l = livingEntity.getLocation();
				location = new Location(effectiveWorld, l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getYaw(), l.getPitch());
			} else {
				MagicSpells.error("GateSpell '" + internalName + "' has invalid coordinates defined!");
				sendMessage(strGateFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			location.setX(location.getX() + .5);
			location.setZ(location.getZ() + .5);
			MagicSpells.debug(3, "Gate location: " + location.toString());
			
			Block b = location.getBlock();
			if (!BlockUtils.isPathable(b) || !BlockUtils.isPathable(b.getRelative(0, 1, 0))) {
				MagicSpells.error("GateSpell '" + internalName + "' has landing spot blocked!");
				sendMessage(strGateFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Location from = livingEntity.getLocation();
			Location to = b.getLocation();
			boolean teleported = livingEntity.teleport(location);
			if (!teleported) {
				MagicSpells.error("GateSpell '" + internalName + "': teleport prevented!");
				sendMessage(strGateFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			playSpellEffects(EffectPosition.CASTER, from);
			playSpellEffects(EffectPosition.TARGET, to);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
