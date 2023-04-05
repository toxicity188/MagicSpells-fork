package com.nisovin.magicspells.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;

public class MagicSpellsPlayerInteractEvent extends PlayerInteractEvent implements IMagicSpellsCompatEvent {

	public MagicSpellsPlayerInteractEvent(Player who, Action action, ItemStack item, Block clickedBlock, BlockFace clickedFace) {
		super(who, action, item, clickedBlock, clickedFace);
	}

}
