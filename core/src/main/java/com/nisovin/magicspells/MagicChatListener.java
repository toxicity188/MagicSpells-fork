package com.nisovin.magicspells;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class MagicChatListener implements Listener {

	private MagicSpells plugin;
	
	MagicChatListener(MagicSpells plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(final AsyncPlayerChatEvent event) {
		MagicSpells.scheduleDelayedTask(() -> handleIncantation(event.getPlayer(), event.getMessage()), 0);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
		boolean casted = handleIncantation(event.getPlayer(), event.getMessage());
		if (casted) event.setCancelled(true);
	}
	
	private boolean handleIncantation(Player player, String message) {
		if (message.contains(" ")) {
			String[] split = message.split(" ");
			Spell spell = MagicSpells.getIncantations().get(split[0].toLowerCase() + " *");
			if (spell != null) {
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				if (spellbook.hasSpell(spell)) {
					String[] args = new String[split.length - 1];
					System.arraycopy(split, 1, args, 0, args.length);
					spell.cast(player, args);
					return true;
				}
				return false;
			}
		}
		Spell spell = MagicSpells.getIncantations().get(message.toLowerCase());
		if (spell != null) {
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			if (spellbook.hasSpell(spell)) {
				spell.cast(player);
				return true;
			}
		}
		return false;
	}
	
}
