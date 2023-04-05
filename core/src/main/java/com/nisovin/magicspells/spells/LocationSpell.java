package com.nisovin.magicspells.spells;

import com.nisovin.magicspells.Spell;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;

public class LocationSpell extends InstantSpell implements SingleSpellSupplier {

	private MagicLocation location;
	
	private Subspell spellToCast;
	private String spellToCastName;

	@Override
	public Spell getSubSpell() {
		return spellToCast != null ? spellToCast.getSpell() : null;
	}

	public LocationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String s = getConfigString("location", "world,0,0,0");
		try {
			String[] split = s.split(",");
			String world = split[0];
			double x = Double.parseDouble(split[1]);
			double y = Double.parseDouble(split[2]);
			double z = Double.parseDouble(split[3]);
			float yaw = 0;
			float pitch = 0;
			if (split.length > 4) yaw = Float.parseFloat(split[4]);
			if (split.length > 5) pitch = Float.parseFloat(split[5]);
			location = new MagicLocation(world, x, y, z, yaw, pitch);
		} catch (Exception e) {
			MagicSpells.error("LocationSpell '" + spellName + "' has an invalid location defined!");
		}

		spellToCastName = getConfigString("spell", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process() || !spellToCast.isTargetedLocationSpell()) {
			MagicSpells.error("LocationSpell '" + internalName + "' has an invalid spell defined!");
			spellToCast = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = location.getLocation();
			if (loc == null) return PostCastAction.ALREADY_HANDLED;

			if (spellToCast != null) spellToCast.castAtLocation(livingEntity, loc, power);
			playSpellEffects(livingEntity, loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
