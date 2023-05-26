package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ReplaceSpell extends TargetedSpell implements TargetedLocationSpell {

	private Map<Block, Material> blocks;

	private List<Material> replace;
	private List<Material> replaceWith;

	private Random random;

	private int yOffset;
	private int radiusUp;
	private int radiusDown;
	private int radiusHoriz;
	private int replaceDuration;

	private boolean pointBlank;
	private boolean replaceRandom;
	private boolean powerAffectsRadius;
	
	public ReplaceSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		blocks = new HashMap<>();
		replace = new ArrayList<>();
		replaceWith = new ArrayList<>();

		random = new Random();
				
		yOffset = getConfigInt("y-offset", 0);
		radiusUp = getConfigInt("radius-up", 1);
		radiusDown = getConfigInt("radius-down", 1);
		radiusHoriz = getConfigInt("radius-horiz", 1);
		replaceDuration = getConfigInt("duration", 0);
		if (replaceDuration < 0) replaceDuration = 0;

		pointBlank = getConfigBoolean("point-blank", false);
		replaceRandom = getConfigBoolean("replace-random", true);
		powerAffectsRadius = getConfigBoolean("power-affects-radius", false);

		List<String> list = getConfigStringList("replace-blocks", null);
		if (list != null) {
			for (String s : list) {
				Material material = Material.getMaterial(s.toUpperCase());
				if (material == null) {
					MagicSpells.error("ReplaceSpell " + internalName + " has an invalid replace-blocks item: " + s);
					continue;
				}

				replace.add(material);
			}
		}

		list = getConfigStringList("replace-with", null);
		if (list != null) {
			for (String s : list) {
				Material material = Material.getMaterial(s.toUpperCase());
				if (material == null) {
					MagicSpells.error("ReplaceSpell " + internalName + " has an invalid replace-with item: " + s);
					continue;
				}

				replaceWith.add(material);
			}
		}
		
		if (!replaceRandom && replace.size() != replaceWith.size()) {
			replaceRandom = true;
			MagicSpells.error("ReplaceSpell " + internalName + " replace-random false, but replace-blocks and replace-with have different sizes!");
		}
		
		if (replace.isEmpty()) MagicSpells.error("ReplaceSpell " + internalName + " has empty replace-blocks list!");
		if (replaceWith.isEmpty()) MagicSpells.error("ReplaceSpell " + internalName + " has empty replace-with list!");
	}

	@Override
	public void turnOff() {
		for (Block b : blocks.keySet()) {
			b.setType(blocks.get(b));
		}

		blocks.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = pointBlank ? livingEntity.getLocation().getBlock() : getTargetedBlock(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			replace(livingEntity, target.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		return replace(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		return replace(null, target, power);
	}

	private boolean replace(LivingEntity caster, Location target, Power power) {
		boolean replaced = false;
		Block block;

		int d = powerAffectsRadius ? Math.round(radiusDown * power.intValue()) : radiusDown;
		int u = powerAffectsRadius ? Math.round(radiusUp * power.intValue()) : radiusUp;
		int h = powerAffectsRadius ? Math.round(radiusHoriz * power.intValue()) : radiusHoriz;

		for (int y = target.getBlockY() - d + yOffset; y <= target.getBlockY() + u + yOffset; y++) {
			for (int x = target.getBlockX() - h; x <= target.getBlockX() + h; x++) {
				for (int z = target.getBlockZ() - h; z <= target.getBlockZ() + h; z++) {
					block = target.getWorld().getBlockAt(x, y, z);
					for (int i = 0; i < replace.size(); i++) {
						if (!replace.get(i).equals(block.getType())) continue;

						blocks.put(block, block.getType());
						Block finalBlock = block;
						if (replaceDuration > 0) MagicSpells.scheduleDelayedTask(() -> {
							finalBlock.setType(blocks.get(finalBlock));
							blocks.remove(finalBlock);
						}, replaceDuration);

						if (replaceRandom) block.setType(replaceWith.get(random.nextInt(replaceWith.size())));
						else block.setType(replaceWith.get(i));

						replaced = true;
						break;
					}
				}
			}
		}

		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);

		return replaced;
	}

}
