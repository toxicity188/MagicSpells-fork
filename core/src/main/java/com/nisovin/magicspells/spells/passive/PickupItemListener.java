package com.nisovin.magicspells.spells.passive;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityPickupItemEvent;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.materials.MagicItemWithNameMaterial;

// Optional trigger variable that is a comma separated list of items to accept
public class PickupItemListener extends PassiveListener {

	Set<Material> materials = new HashSet<>();
	Map<MagicMaterial, List<PassiveSpell>> types = new HashMap<>();
	List<PassiveSpell> allTypes = new ArrayList<>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		if (var == null || var.isEmpty()) {
			allTypes.add(spell);
		} else {
			String[] split = var.split(",");
			for (String s : split) {
				s = s.trim();
				MagicMaterial mat;
				if (s.contains("|")) {
					String[] stuff = s.split("\\|");
					mat = MagicSpells.getItemNameResolver().resolveItem(stuff[0]);
					if (mat != null) mat = new MagicItemWithNameMaterial(mat, stuff[1]);						
				} else {
					mat = MagicSpells.getItemNameResolver().resolveItem(s);
				}
				if (mat != null) {
					List<PassiveSpell> list = types.computeIfAbsent(mat, material -> new ArrayList<>());
					list.add(spell);
					materials.add(mat.getMaterial());
				}
			}	
		}		
	}
	
	@OverridePriority
	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player)) return;
		Player pl = (Player) event.getEntity();
		if (!allTypes.isEmpty()) {
			Spellbook spellbook = MagicSpells.getSpellbook(pl);
			for (PassiveSpell spell : allTypes) {
				if (!isCancelStateOk(spell, event.isCancelled())) continue;
				if (!spellbook.hasSpell(spell)) continue;
				boolean casted = spell.activate(pl);
				if (!PassiveListener.cancelDefaultAction(spell, casted)) continue;
				event.setCancelled(true);
			}
		}
		
		if (!types.isEmpty()) {
			List<PassiveSpell> list = getSpells(event.getItem().getItemStack());
			if (list != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(pl);
				for (PassiveSpell spell : list) {
					if (!isCancelStateOk(spell, event.isCancelled())) continue;
					if (!spellbook.hasSpell(spell)) continue;
					boolean casted = spell.activate(pl);
					if (!PassiveListener.cancelDefaultAction(spell, casted)) continue;
					event.setCancelled(true);
				}
			}
		}
	}
	
	private List<PassiveSpell> getSpells(ItemStack item) {
		if (!materials.contains(item.getType())) return null;
		for (Entry<MagicMaterial, List<PassiveSpell>> entry : types.entrySet()) {
			if (entry.getKey().equals(item)) return entry.getValue();
		}
		return null;
	}

}
