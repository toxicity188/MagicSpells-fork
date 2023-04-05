package com.nisovin.magicspells.spells.command;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;

public class KeybindSpell extends CommandSpell {

	private Map<String, Keybinds> playerKeybinds;

	private ItemStack wandItem;
	private ItemStack defaultSpellIcon;

	public KeybindSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		playerKeybinds = new HashMap<>();

		wandItem = Util.getItemStackFromString(getConfigString("wand-item", "blaze_rod"));
		defaultSpellIcon = Util.getItemStackFromString(getConfigString("default-spell-icon", "redstone"));
	}
	
	@Override
	protected void initialize() {
		super.initialize();

		Util.forEachPlayerOnline(this::loadKeybinds);
	}

	private void loadKeybinds(Player player) {
		File file = new File(MagicSpells.plugin.getDataFolder(), "spellbooks" + File.separator + "keybinds-" + player.getName().toLowerCase() + ".txt");
		if (!file.exists()) return;
		try {
			Keybinds keybinds = new Keybinds(player);
			YamlConfiguration conf = new YamlConfiguration();
			conf.load(file);
			for (String key : conf.getKeys(false)) {
				int slot = Integer.parseInt(key);
				String spellName = conf.getString(key);
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null) keybinds.setKeybind(slot, spell);
			}
			playerKeybinds.put(player.getName(), keybinds);
		} catch (Exception e) {
			MagicSpells.plugin.getLogger().severe("Failed to load player keybinds for " + player.getName());
			e.printStackTrace();
		}

	}
	
	private void saveKeybinds(Keybinds keybinds) {		
		File file = new File(MagicSpells.plugin.getDataFolder(), "spellbooks" + File.separator + "keybinds-" + keybinds.player.getName().toLowerCase() + ".txt");
		YamlConfiguration conf = new YamlConfiguration();
		Spell[] binds = keybinds.keybinds;
		for (int i = 0; i < binds.length; i++) {
			if (binds[i] == null) continue;
			conf.set(i + "", binds[i].getInternalName());
		}
		try {
			conf.save(file);
		} catch (IOException e) {
			MagicSpells.plugin.getLogger().severe("Failed to save keybinds for " + keybinds.player.getName());
			e.printStackTrace();
		}
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args.length != 1) {
				player.sendMessage("invalid args");
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Keybinds keybinds = playerKeybinds.computeIfAbsent(player.getName(), name -> new Keybinds(player));
			
			int slot = player.getInventory().getHeldItemSlot();
			ItemStack item = player.getEquipment().getItemInMainHand();
			
			if (args[0].equalsIgnoreCase("clear")) {
				keybinds.clearKeybind(slot);
				saveKeybinds(keybinds);
				return PostCastAction.HANDLE_NORMALLY;
			}
			if (args[0].equalsIgnoreCase("clearall")) {
				keybinds.clearKeybinds();
				saveKeybinds(keybinds);
				return PostCastAction.HANDLE_NORMALLY;
			}
			if (item != null && !BlockUtils.isAir(item.getType())) {
				player.sendMessage("not empty");
				return PostCastAction.ALREADY_HANDLED;
			}

			Spell spell = MagicSpells.getSpellbook(player).getSpellByName(args[0]);
			if (spell == null) {
				player.sendMessage("no spell");
				return PostCastAction.ALREADY_HANDLED;
			}

			keybinds.setKeybind(slot, spell);
			keybinds.select(slot);
			saveKeybinds(keybinds);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	@EventHandler
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;
		keybinds.deselect(event.getPreviousSlot());
		keybinds.select(event.getNewSlot());
	}
	
	@EventHandler
	public void onAnimate(PlayerAnimationEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;
		boolean casted = keybinds.castKeybind(event.getPlayer().getInventory().getHeldItemSlot());
		if (casted) event.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;
		
		if (keybinds.hasKeybind(event.getPlayer().getInventory().getHeldItemSlot())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		loadKeybinds(event.getPlayer());
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
	private class Keybinds {
		
		private Player player;
		private Spell[] keybinds;

		private Keybinds(Player player) {
			this.player = player;
			this.keybinds = new Spell[10];
		}

		private void deselect(int slot) {
			Spell spell = keybinds[slot];
			if (spell == null) return;
			ItemStack spellIcon = spell.getSpellIcon();
			if (spellIcon == null) spellIcon = defaultSpellIcon;
			MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, spellIcon);
		}

		private void select(int slot) {
			Spell spell = keybinds[slot];
			if (spell == null) return;
			MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, wandItem);
		}

		private boolean hasKeybind(int slot) {
			return keybinds[slot] != null;
		}

		private boolean castKeybind(int slot) {
			Spell spell = keybinds[slot];
			if (spell == null) return false;
			spell.cast(player);
			return true;
		}

		private void setKeybind(int slot, Spell spell) {
			keybinds[slot] = spell;
		}

		private void clearKeybind(int slot) {
			keybinds[slot] = null;
			MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, null);
		}

		private void clearKeybinds() {
			for (int i = 0; i < keybinds.length; i++) {
				if (keybinds[i] == null) continue;
				keybinds[i] = null;
				MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, i, null);
			}
		}
		
	}

}
