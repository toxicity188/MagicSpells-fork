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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;

// Trigger variable accepts a comma separated list of blocks to accept
public class RightClickBlockTypeListener extends PassiveListener {

	Set<Material> materials = new HashSet<>();
	Map<Material, List<PassiveSpell>> types = new HashMap<>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		String[] split = var.split(",");
		for (String s : split) {
			s = s.trim();
			Material m = Material.getMaterial(s.toUpperCase());
			if (m == null) {
				MagicSpells.error("Invalid type on rightclickblocktype trigger '" + var + "' on passive spell '" + spell.getInternalName() + '\'');
				continue;
			}
			List<PassiveSpell> list = types.computeIfAbsent(m, material -> new ArrayList<>());
			list.add(spell);
			materials.add(m);
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onRightClick(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		List<PassiveSpell> list = getSpells(event.getClickedBlock());
		if (list != null) {
			Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
			for (PassiveSpell spell : list) {
				if (!PassiveListener.isCancelStateOk(spell, event.isCancelled())) continue;
				if (!spellbook.hasSpell(spell, false)) continue;
				boolean casted = spell.activate(event.getPlayer(), event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
				if (!PassiveListener.cancelDefaultAction(spell, casted)) continue;
				event.setCancelled(true);
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
