package com.nisovin.magicspells.util;

import java.util.List;
import java.util.ArrayList;

import com.nisovin.magicspells.util.wrapper.BlockUtilsWrapper;
import com.nisovin.magicspells.util.wrapper.MaterialWrapper;
import com.nisovin.magicspells.util.wrapper.WrappedMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;

public class BlockUtils {

	private static final BlockUtilsWrapper WRAPPER = getWrapper();
	private static BlockUtilsWrapper getWrapper() {
		try {
			switch (MaterialWrapper.getVersion()) {
				case LEGACY:
					return (BlockUtilsWrapper) Class.forName("com.nisovin.magicspells.util.wrapper.LegacyBlockUtils").getDeclaredConstructor().newInstance();
				case CURRENT:
					return (BlockUtilsWrapper) Class.forName("com.nisovin.magicspells.util.wrapper.CurrentBlockUtils").getDeclaredConstructor().newInstance();
			}
		} catch (Exception ignored) {}
		return null;
	}

	public static List<Block> getNearbyBlocks(Location location, int radius, int height) {
		List<Block> blocks = new ArrayList<>();
		for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
			for (int y = location.getBlockY() - height; y <= location.getBlockY() + height; y++) {
				for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
					blocks.add(location.getWorld().getBlockAt(x, y, z));
				}
			}
		}
		return blocks;
	}

	public static boolean isTransparent(Spell spell, Block block) {
		return spell.getLosTransparentBlocks().contains(block.getType());
	}

	public static Block getTargetBlock(Spell spell, LivingEntity entity, int range) {
		try {
			if (spell != null) return entity.getTargetBlock(spell.getLosTransparentBlocks(), range);
			return entity.getTargetBlock(MagicSpells.getTransparentBlocks(), range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
	}

	public static List<Block> getLastTwoTargetBlock(Spell spell, LivingEntity entity, int range) {
		try {
			return entity.getLastTwoTargetBlocks(spell.getLosTransparentBlocks(), range);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return null;
		}
	}

	public static void setBlockFromFallingBlock(Block block, FallingBlock fallingBlock, boolean physics) {
		byte blockData = fallingBlock.getBlockData();
		block.setType(fallingBlock.getMaterial(),physics);
		block.setData(blockData, physics);
	}

	public static int getWaterLevel(BlockState blockState) {
		return ((Levelled) blockState).getLevel();
	}

	public static boolean isPathable(Block block) {
		return isPathable(block.getType());
	}

	public static boolean isAir(Material m) {
		return
				m == Material.AIR ||
						m == WrappedMaterial.getWrapper().getCaveAir() ||
						m == WrappedMaterial.getWrapper().getVoidAir();
	}

	public static boolean isPathable(Material mat) {
		return (WRAPPER != null && WRAPPER.isPathable(mat));
	}

	public static boolean isSafeToStand(Location location) {
		if (!isPathable(location.getBlock())) return false;
		if (!isPathable(location.add(0, 1, 0).getBlock())) return false;
		return !isPathable(location.subtract(0, 2, 0).getBlock()) || !isPathable(location.subtract(0, 1, 0).getBlock());
	}

}
