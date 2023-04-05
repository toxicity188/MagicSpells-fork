package com.nisovin.magicspells.spelleffects;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.nisovin.magicspells.util.wrapper.WrappedMaterial;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.SpellAnimation;

public class NovaEffect extends SpellEffect {

	private Material material;

	private double range;

	private int radius;
	private int startRadius;
	private int heightPerTick;
	private int expandInterval;
	private int expandingRadiusChange;

	private boolean circleShape;
	private boolean removePreviousBlocks;

	@Override
	public void loadFromString(String string) {
		super.loadFromString(string);
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {

		String materialName = config.getString("type", "fire").toUpperCase();
		material = Material.getMaterial(materialName);

		if (material == null || !material.isBlock()) {
			material = null;
			MagicSpells.error("Wrong nova type defined! '" + materialName + "'");
		}

		range = Math.max(config.getDouble("range", 20), 1);

		radius = config.getInt("radius", 3);
		startRadius = config.getInt("start-radius", 0);
		heightPerTick = config.getInt("height-per-tick", 0);
		expandInterval = config.getInt("expand-interval", 5);
		expandingRadiusChange = config.getInt("expanding-radius-change", 1);
		if (expandingRadiusChange < 1) expandingRadiusChange = 1;

		circleShape = config.getBoolean("circle-shape", false);
		removePreviousBlocks = config.getBoolean("remove-previous-blocks", true);

	}

	@Override
	public Runnable playEffectLocation(Location location) {
		if (material == null) return null;

		// Get nearby players
		Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, range, range, range);
		List<Player> nearby = new ArrayList<>();
		for (Entity e : nearbyEntities) {
			if (!(e instanceof Player)) continue;
			nearby.add((Player)e);
		}

		// Start animation
		if (!circleShape) new NovaAnimationSquare(nearby, location.getBlock(), material, radius, expandInterval, expandingRadiusChange);
		else new NovaAnimationCircle(nearby, location.getBlock(), material, radius, expandInterval, expandingRadiusChange);
		return null;
	}

	private class NovaAnimationSquare extends SpellAnimation {

		List<Player> nearby;
		Set<Block> blocks;
		Block center;
		Material matNova;
		int radiusNova;
		int radiusChange;

		public NovaAnimationSquare(List<Player> nearby, Block center, Material mat, int radius, int tickInterval, int activeRadiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
			this.radiusChange = activeRadiusChange;
		}

		@Override
		protected void onTick(int tick) {
			tick += startRadius;
			tick *= radiusChange;

			// Remove old blocks
			if (removePreviousBlocks) {
				for (Block b : blocks) {
					for (Player p : nearby) {
						sendBlockChange(p,b.getLocation(),WrappedMaterial.getWrapper().getTallGrass());
					}
				}
				blocks.clear();
			}

			if (tick > radiusNova + 1) {
				stop();
				return;
			} else if (tick > radiusNova) {
				return;
			}

			// Set next ring
			int bx = center.getX();
			int y = center.getY();
			int bz = center.getZ();
			y += tick * heightPerTick;

			for (int x = bx - tick; x <= bx + tick; x++) {
				for (int z = bz - tick; z <= bz + tick; z++) {
					if (Math.abs(x - bx) != tick && Math.abs(z - bz) != tick) continue;

					Block b = center.getWorld().getBlockAt(x, y, z);
					if (BlockUtils.isAir(b.getType()) || b.getType() == WrappedMaterial.getWrapper().getTallGrass()) {
						Block under = b.getRelative(BlockFace.DOWN);
						if (BlockUtils.isAir(under.getType()) || under.getType() == WrappedMaterial.getWrapper().getTallGrass()) b = under;
					} else if (BlockUtils.isAir(b.getRelative(BlockFace.UP).getType()) || b.getRelative(BlockFace.UP).getType() == WrappedMaterial.getWrapper().getTallGrass()) {
						b = b.getRelative(BlockFace.UP);
					}

					if (!BlockUtils.isAir(b.getType()) && b.getType() != WrappedMaterial.getWrapper().getTallGrass()) continue;

					if (blocks.contains(b)) continue;
					for (Player p : nearby) {
						sendBlockChange(p,b.getLocation(),matNova);
					}
					blocks.add(b);
				}
			}
		}

		@Override
		protected void stop() {
			super.stop();

			for (Block b : blocks) {
				for (Player p : nearby) {
					sendBlockChange(p,b.getLocation(),WrappedMaterial.getWrapper().getTallGrass());
				}
			}

			blocks.clear();
		}

	}

	private class NovaAnimationCircle extends SpellAnimation {

		List<Player> nearby;
		Set<Block> blocks;
		Block center;
		Material matNova;
		int radiusNova;
		int radiusChange;

		public NovaAnimationCircle(List<Player> nearby, Block center, Material mat, int radius, int tickInterval, int activeRadiusChange) {
			super(tickInterval, true);
			this.nearby = nearby;
			this.center = center;
			this.matNova = mat;
			this.radiusNova = radius;
			this.blocks = new HashSet<>();
			this.radiusChange = activeRadiusChange;
		}

		@Override
		protected void onTick(int tick) {
			tick += startRadius;
			tick *= radiusChange;

			// Remove old blocks
			if (removePreviousBlocks) {
				for (Block b : blocks) {
					for (Player p : nearby){
						sendBlockChange(p,b.getLocation(),b.getType());
					}
				}
				blocks.clear();
			}

			if (tick > radiusNova + 1) {
				stop();
				return;
			} else if (tick > radiusNova) {
				return;
			}

			// Generate the bottom block
			Location centerLocation = center.getLocation().clone();
			centerLocation.add(0.5, tick * heightPerTick, 0.5);
			Block b;

			if (startRadius == 0 && tick == 0) {
				b = centerLocation.getWorld().getBlockAt(centerLocation);
				if (BlockUtils.isAir(b.getType()) || b.getType() == WrappedMaterial.getWrapper().getTallGrass()) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (BlockUtils.isAir(under.getType()) || under.getType() == WrappedMaterial.getWrapper().getTallGrass()) b = under;
				} else if (BlockUtils.isAir(b.getRelative(BlockFace.UP).getType()) || b.getRelative(BlockFace.UP).getType() == WrappedMaterial.getWrapper().getTallGrass()) {
					b = b.getRelative(BlockFace.UP);
				}

				if (!BlockUtils.isAir(b.getType()) && b.getType() != WrappedMaterial.getWrapper().getTallGrass()) return;

				if (blocks.contains(b)) return;
				for (Player p : nearby) {
					sendBlockChange(p,b.getLocation(),matNova);
				}
				blocks.add(b);
			}

			// Generate the circle
			Vector v;
			double angle, x, z;
			double amount = tick * 64;
			double inc = (2 * Math.PI) / amount;
			for (int i = 0; i < amount; i++) {
				angle = i * inc;
				x = tick * Math.cos(angle);
				z = tick * Math.sin(angle);
				v = new Vector(x, 0, z);
				b = center.getWorld().getBlockAt(centerLocation.add(v));
				centerLocation.subtract(v);

				if (BlockUtils.isAir(b.getType()) || b.getType() == WrappedMaterial.getWrapper().getTallGrass()) {
					Block under = b.getRelative(BlockFace.DOWN);
					if (BlockUtils.isAir(under.getType()) || under.getType() == WrappedMaterial.getWrapper().getTallGrass()) b = under;
				} else if (BlockUtils.isAir(b.getRelative(BlockFace.UP).getType()) || b.getRelative(BlockFace.UP).getType() == WrappedMaterial.getWrapper().getTallGrass()) {
					b = b.getRelative(BlockFace.UP);
				}

				if (!BlockUtils.isAir(b.getType()) && b.getType() != WrappedMaterial.getWrapper().getTallGrass()) continue;

				if (blocks.contains(b)) continue;
				for (Player p : nearby) {
					sendBlockChange(p,b.getLocation(),matNova);
				}
				blocks.add(b);
			}

		}

		@Override
		protected void stop() {
			super.stop();

			for (Block b : blocks) {
				for (Player p : nearby) {
					sendBlockChange(p,b.getLocation(),b.getType());
				}
			}

			blocks.clear();
		}

	}
	private static void sendBlockChange(Player player, Location location, Material material) {
		ProtocolManager manager = ProtocolLibrary.getProtocolManager();
		PacketContainer container = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
		container.getBlockData().write(0, WrappedBlockData.createData(material));
		container.getBlockPositionModifier().write(0,new BlockPosition(location.getBlockX(),location.getBlockY(),location.getBlockZ()));
		try {
			manager.sendServerPacket(player,container);
		} catch (Exception ignored) {}
	}

}
