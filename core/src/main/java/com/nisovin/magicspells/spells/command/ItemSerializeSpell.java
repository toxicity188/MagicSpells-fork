package com.nisovin.magicspells.spells.command;

import com.nisovin.magicspells.power.Power;

import java.io.File;
import java.util.List;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.InventoryUtil;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.itemreader.alternative.AlternativeReaderManager;

// TODO find a good way of configuring which items to serialize
// TODO make the serialization and io processing async
// TODO reconsider the way of determining file name
// TODO produce a better way of naming the items
// TODO allow a configurable yaml header with variable substitution
// TODO allow configurable messages
// WARNING: THIS SPELL IS SUBJECT TO BREAKING CHANGES
// DO NOT USE CURRENTLY IF EXPECTING LONG TERM UNCHANGING BEHAVIOR
public class ItemSerializeSpell extends CommandSpell {
	
	private File dataFolder;

	private String serializerKey;

	private int indentation;
	
	public ItemSerializeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		serializerKey = getConfigString("serializer-key", "external::spigot");

		indentation = getConfigInt("indentation", 4);
	}
	
	@Override
	protected void initialize() {
		// Setup data folder
		dataFolder = new File(MagicSpells.getInstance().getDataFolder(), "items");
		if (!dataFolder.exists()) dataFolder.mkdirs();
	}
	
	@Override
	protected void turnOff() {
		super.turnOff();
		
		// This is where any resources should be closed if they aren't already
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (player == null) {
				// TODO send error message
				return PostCastAction.ALREADY_HANDLED;
			}
			
			ItemStack heldItem = player.getInventory().getItemInMainHand();
			if (InventoryUtil.isNothing(heldItem)) {
				player.sendMessage("You must be holding an item in your hand");
				return PostCastAction.ALREADY_HANDLED;
			}
			
			processItem(heldItem);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	private File makeFile() {
		File file = new File(dataFolder, System.currentTimeMillis() + ".yml");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}
	
	private void processItem(ItemStack itemStack) {
		ConfigurationSection section = AlternativeReaderManager.serialize(serializerKey, itemStack);
		File file = makeFile();
		YamlConfiguration outputYaml = new YamlConfiguration();
		outputYaml.set("predefined-items." + System.currentTimeMillis(), section);
		outputYaml.options().indent(indentation);
		try {
			outputYaml.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
}
