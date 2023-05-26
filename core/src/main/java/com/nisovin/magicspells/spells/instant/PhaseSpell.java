package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PhaseSpell extends InstantSpell {

	private List<Material> phasableBlocks;

	private String strCantPhase;

	private int maxDistance;

	public PhaseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		List<String> phasable = getConfigStringList("phasable-blocks", null);
		phasableBlocks = new ArrayList<>();
		if (phasable != null && !phasable.isEmpty()) {
			for (String s : phasable) {
				Material material = Material.getMaterial(s.toUpperCase());
				if (material == null) continue;
				phasableBlocks.add(material);
			}
		}

		maxDistance = getConfigInt("max-distance", 15);

		strCantPhase = getConfigString("str-cant-phase", "Unable to find place to phase to.");
		
		if (phasableBlocks.isEmpty()) phasableBlocks = null;
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int r = Math.round(range * power.intValue());
			int distance = Math.round(maxDistance * power.intValue());
			
			BlockIterator iter;
			try {
				iter = new BlockIterator(livingEntity, distance << 1);
			} catch (IllegalStateException e) {
				sendMessage(strCantPhase, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			int i = 0;
			Block start = null;
			Location location = null;
			
			while (start == null && i++ < r << 1 && iter.hasNext()) {
				Block b = iter.next();
				if (BlockUtils.isAir(b.getType())) continue;
				if (livingEntity.getLocation().distanceSquared(b.getLocation()) >= r * r) continue;
				start = b;
				break;
			}
			
			if (start != null) {
				if (canPassThrough(start)) {
					Block end = null;
					while (end == null && i++ < distance << 1 && iter.hasNext()) {
						Block b = iter.next();
						if (BlockUtils.isAir(b.getType()) && BlockUtils.isAir(b.getRelative(0, 1, 0).getType())
								&& livingEntity.getLocation().distanceSquared(b.getLocation()) < distance * distance) {
							location = b.getLocation();
							break;
						}
						if (!canPassThrough(b)) {
							location = null;
							break;
						}
					}
				} else location = null;
			}
			
			if (location == null) {
				sendMessage(strCantPhase, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			location.setX(location.getX() + 0.5);
			location.setZ(location.getZ() + 0.5);
			location.setPitch(livingEntity.getLocation().getPitch());
			location.setYaw(livingEntity.getLocation().getYaw());
			playSpellEffects(EffectPosition.CASTER, livingEntity.getLocation());
			playSpellEffects(EffectPosition.TARGET, location);
			livingEntity.teleport(location);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean canPassThrough(Block block) {
		if (phasableBlocks == null) return true;
		if (phasableBlocks.isEmpty()) return true;
		for (Material mat : phasableBlocks) {
			if (mat.equals(block.getType())) return true;
		}
		return false;
	}

}
