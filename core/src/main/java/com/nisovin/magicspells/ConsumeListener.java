package com.nisovin.magicspells;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.Spell.SpellCastResult;

public class ConsumeListener implements Listener {

	private MagicSpells plugin;

	private Map<CastItem, Spell> consumeCastItems = new HashMap<>();
	private HashMap<String, Long> lastCast = new HashMap<>();
	
	ConsumeListener(MagicSpells plugin) {
		this.plugin = plugin;
		for (Spell spell : MagicSpells.getSpells().values()) {
			CastItem[] items = spell.getConsumeCastItems();
			if (items.length <= 0) continue;
			for (CastItem item : items) {
				Spell old = consumeCastItems.put(item, spell);
				if (old == null) continue;
				MagicSpells.error("The spell '" + spell.getInternalName() + "' has same consume-cast-item as '" + old.getInternalName() + "'!");
			}
		}
	}
	
	boolean hasConsumeCastItems() {
		return !consumeCastItems.isEmpty();
	}
	
	@EventHandler
	public void onConsume(final PlayerItemConsumeEvent event) {
		CastItem castItem = new CastItem(event.getItem());
		final Spell spell = consumeCastItems.get(castItem);
		if (spell == null) return;

		Player player = event.getPlayer();
		Long lastCastTime = lastCast.get(player.getName());
		if (lastCastTime != null && lastCastTime + plugin.globalCooldown > System.currentTimeMillis()) return;
		lastCast.put(player.getName(), System.currentTimeMillis());

		if (MagicSpells.getSpellbook(player).canCast(spell)) {
			SpellCastResult result = spell.cast(event.getPlayer());
			if (result.state != SpellCastState.NORMAL) event.setCancelled(true);
		}
	}

}
