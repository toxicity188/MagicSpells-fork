package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;

// Optional trigger variable of comma separated list of blocks to accept
public class BlockPlaceListener extends PassiveListener {

	Set<Material> materials = new HashSet<>();
	Map<Material, List<PassiveSpell>> types = new HashMap<>();
	List<PassiveSpell> allTypes = new ArrayList<>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(spell);
			return;
		}
		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();
			Material m = Material.getMaterial(s.toUpperCase());
			if (m == null) continue;
			List<PassiveSpell> list = types.computeIfAbsent(m, material -> new ArrayList<>());
			list.add(spell);
			materials.add(m);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
		if (!allTypes.isEmpty()) {
			for (PassiveSpell spell : allTypes) {
				if (!isCancelStateOk(spell, event.isCancelled())) continue;
				if (!spellbook.hasSpell(spell, false)) continue;
				boolean casted = spell.activate(event.getPlayer(), event.getBlock().getLocation().add(0.5, 0.5, 0.5));
				if (PassiveListener.cancelDefaultAction(spell, casted)) event.setCancelled(true);
			}
		}
		if (!types.isEmpty()) {
			List<PassiveSpell> list = getSpells(event.getBlock());
			if (list != null) {
				for (PassiveSpell spell : list) {
					if (!isCancelStateOk(spell, event.isCancelled())) continue;
					if (!spellbook.hasSpell(spell, false)) continue;
					boolean casted = spell.activate(event.getPlayer(), event.getBlock().getLocation().add(0.5, 0.5, 0.5));
					if (PassiveListener.cancelDefaultAction(spell, casted)) event.setCancelled(true);
				}
			}
		}
	}

	private List<PassiveSpell> getSpells(Block block) {
		if (!materials.contains(block.getType())) return null;
		for (Entry<Material, List<PassiveSpell>> entry : types.entrySet()) {
			if (entry.getKey().equals(block.getType())) return entry.getValue();
		}
		return null;
	}

}
