package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.nisovin.magicspells.util.wrapper.WrappedMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class LightwalkSpell extends BuffSpell {
	
	private Map<UUID, Block> lightwalkers;
	private Set<Material> allowedTypes;
	private Material material;

	public LightwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		String materialName = getConfigString("material", "GLOWSTONE").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null) {
			MagicSpells.error("LightwalkSpell " + internalName + " has an invalid material defined");
			material = Material.GLOWSTONE;
		}

		lightwalkers = new HashMap<>();
		allowedTypes = new HashSet<>();

		List<String> blockList = getConfigStringList("allowed-types", null);
		if (blockList != null) {
			for (String str : blockList) {
				Material material = Material.getMaterial(str.toUpperCase());
				if (material == null) MagicSpells.error("LightwalkSpell " + internalName + " has an invalid block defined " + str);
				else allowedTypes.add(material);
			}
		} else {
			allowedTypes.add(WrappedMaterial.getWrapper().getGrass());
			allowedTypes.add(Material.DIRT);
			allowedTypes.add(Material.GRAVEL);
			allowedTypes.add(Material.STONE);
			allowedTypes.add(Material.NETHERRACK);
			allowedTypes.add(Material.SOUL_SAND);
			allowedTypes.add(Material.SAND);
			allowedTypes.add(Material.SANDSTONE);
			allowedTypes.add(Material.GLASS);
			allowedTypes.add(WrappedMaterial.getWrapper().getWool());
			allowedTypes.add(Material.BRICK);
			allowedTypes.add(Material.OBSIDIAN);
			allowedTypes.add(WrappedMaterial.getWrapper().getWood());
			allowedTypes.add(WrappedMaterial.getWrapper().getLog());
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, Power power, String[] args) {
		lightwalkers.put(entity.getUniqueId(), entity.getLocation().getBlock().getRelative(BlockFace.DOWN));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return lightwalkers.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		Block b = lightwalkers.remove(entity.getUniqueId());
		if (b == null) return;
		if (!(entity instanceof Player)) return;
		sendBlockChange((Player) entity,b.getLocation(),b.getType());
	}

	@Override
	protected void turnOff() {
		for (UUID id : lightwalkers.keySet()) {
			Entity entity = Bukkit.getEntity(id);
			if (!(entity instanceof Player)) continue;
			Block b = lightwalkers.get(id);
			if (b == null) continue;
			sendBlockChange((Player) entity,b.getLocation(),b.getType());
		}

		lightwalkers.clear();
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (!isActive(player)) return;

		Block oldBlock = lightwalkers.get(player.getUniqueId());
		Block newBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		if (oldBlock == null) return;
		if (newBlock == null) return;
		if (oldBlock.equals(newBlock)) return;
		if (!allowedTypes.contains(newBlock.getType())) return;
		if (BlockUtils.isAir(newBlock.getType())) return;
		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		addUseAndChargeCost(player);
		lightwalkers.put(player.getUniqueId(), newBlock);
		sendBlockChange(player,newBlock.getLocation(),material);
		if (oldBlock != null) sendBlockChange(player,oldBlock.getLocation(),oldBlock.getType());
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
