package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class UnconjureSpell extends InstantSpell {

	private List<String> itemNames;
	private List<ItemStack> items;
	
	public UnconjureSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		itemNames = getConfigStringList("items", null);
	}
	
	@Override
	public void initialize() {
		items = new ArrayList<>();

		if (itemNames == null) return;
		for (String s : itemNames) {
			ItemStack i = Util.getItemStackFromString(s);
			if (i == null) continue;
			items.add(i);
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			ItemStack[] inv = player.getInventory().getContents();
			if (check(inv)) player.getInventory().setContents(inv);
			ItemStack[] armor = player.getInventory().getArmorContents();
			if (check(armor)) player.getInventory().setArmorContents(armor);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean check(ItemStack[] inv) {
		boolean ch = false;
		for (int i = 0; i < inv.length; i++) {
			if (inv[i] == null) continue;
			for (ItemStack item : items) {
				if (!item.isSimilar(inv[i])) continue;
				inv[i] = null;
				ch = true;
				break;
			}
		}
		return ch;
	}

}
