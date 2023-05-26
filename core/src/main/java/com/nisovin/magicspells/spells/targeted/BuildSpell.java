package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

public class BuildSpell extends TargetedSpell implements TargetedLocationSpell {

	private Set<Material> allowedTypes;

	private String strCantBuild;
	private String strInvalidBlock;

	private int slot;

	private boolean consumeBlock;
	private boolean checkPlugins;
	private boolean playBreakEffect;

	public BuildSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strCantBuild = getConfigString("str-cant-build", "You can't build there.");
		strInvalidBlock = getConfigString("str-invalid-block", "You can't build that block.");

		slot = getConfigInt("slot", 0);

		consumeBlock = getConfigBoolean("consume-block", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		playBreakEffect = getConfigBoolean("show-effect", true);

		List<String> materials = getConfigStringList("allowed-types", null);
		if (materials == null) {
			materials = new ArrayList<>();
			materials.add("GRASS_BLOCK");
			materials.add("STONE");
			materials.add("DIRT");
		}

		allowedTypes = new HashSet<>();
		for (String str : materials) {
			Material material = Material.getMaterial(str.toUpperCase());
			if (material == null) {
				MagicSpells.error("BuildSpell '" + internalName + "' has an invalid material '" + str + "' defined!");
				continue;
			}
			if (!material.isBlock()) {
				MagicSpells.error("BuildSpell '" + internalName + "' has a non block material '" + str + "' defined!");
				continue;
			}

			allowedTypes.add(material);
		}
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			ItemStack item = player.getInventory().getItem(slot);
			if (item == null || !isAllowed(item.getType())) return noTarget(player, strInvalidBlock);
			
			List<Block> lastBlocks;
			try {
				lastBlocks = getLastTwoTargetedBlocks(player, power);
			} catch (IllegalStateException e) {
				DebugHandler.debugIllegalState(e);
				lastBlocks = null;
			}

			if (lastBlocks == null || lastBlocks.size() < 2 || BlockUtils.isAir(lastBlocks.get(1).getType())) return noTarget(player, strCantBuild);

			boolean built = build(player, lastBlocks.get(0), lastBlocks.get(1), item);
			if (!built) return noTarget(player, strCantBuild);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		if (!(caster instanceof Player)) return false;
		ItemStack item = ((Player) caster).getInventory().getItem(slot);
		if (item == null || !isAllowed(item.getType())) return false;

		Block block = target.getBlock();

		return build((Player) caster, block, block, item);
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		return false;
	}

	private boolean isAllowed(Material mat) {
		if (!mat.isBlock()) return false;
		if (allowedTypes == null) return false;
		return allowedTypes.contains(mat);
	}

	private boolean build(Player player, Block block, Block against, ItemStack item) {
		BlockState previousState = block.getState();
		block.setType(item.getType());

		if (checkPlugins) {
			MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(block, previousState, against, player.getEquipment().getItemInMainHand(), player, true);
			EventUtil.call(event);
			if (event.isCancelled() && block.getType() == item.getType()) {
				previousState.update(true);
				return false;
			}
		}

		if (playBreakEffect) block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());

		playSpellEffects(player, block.getLocation());

		if (consumeBlock) {
			int amt = item.getAmount() - 1;
			if (amt > 0) {
				item.setAmount(amt);
				player.getInventory().setItem(slot, item);
			} else player.getInventory().setItem(slot, null);
		}

		return true;
	}

}
