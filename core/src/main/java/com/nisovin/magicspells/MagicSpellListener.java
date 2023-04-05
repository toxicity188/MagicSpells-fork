package com.nisovin.magicspells;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.events.SpellTargetEvent;

class MagicSpellListener implements Listener {
		
	MagicSpellListener(MagicSpells plugin) {
		// No op
	}

	@EventHandler
	public void onSpellTarget(SpellTargetEvent event) {
		// Check if target has notarget permission or spectator gamemode
		LivingEntity target = event.getTarget();
		if (!(target instanceof Player)) return;
		if (Perm.NOTARGET.has(target)) event.setCancelled(true);
		if (((Player) target).getGameMode() == GameMode.SPECTATOR) event.setCancelled(true);
	}
	
}
