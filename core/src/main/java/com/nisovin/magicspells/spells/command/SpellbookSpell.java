package com.nisovin.magicspells.spells.command;

import com.nisovin.magicspells.power.Power;

import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.io.FileWriter;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.util.regex.Pattern;
import java.io.FileNotFoundException;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;

// Advanced perm is for being able to destroy spellbooks
// Op is currently required for using the reload

public class SpellbookSpell extends CommandSpell {
	
	private static final Pattern PATTERN_CAST_ARG_USAGE = Pattern.compile("^[0-9]+$");

	private ArrayList<String> bookSpells;
	private ArrayList<Integer> bookUses;
	private ArrayList<MagicLocation> bookLocations;

	private Material spellbookBlock;
	private String spellbookBlockName;

	private int defaultUses;

	private boolean destroyBookcase;

	private String strUsage;
	private String strNoSpell;
	private String strLearned;
	private String strNoTarget;
	private String strCantTeach;
	private String strCantLearn;
	private String strLearnError;
	private String strCantDestroy;
	private String strHasSpellbook;
	private String strAlreadyKnown;
	
	public SpellbookSpell(MagicConfig config, String spellName) {
		super(config,spellName);

		bookSpells = new ArrayList<>();
		bookUses = new ArrayList<>();
		bookLocations = new ArrayList<>();

		spellbookBlockName = getConfigString("spellbook-block", "bookshelf").toUpperCase();
		spellbookBlock = Material.getMaterial(spellbookBlockName);
		if (spellbookBlock == null || !spellbookBlock.isBlock()) {
			MagicSpells.error("SpellbookSpell '" + internalName + "' has an invalid spellbook-block defined!");
			spellbookBlock = null;
		}

		defaultUses = getConfigInt("default-uses", -1);

		destroyBookcase = getConfigBoolean("destroy-when-used-up", false);

		strUsage = getConfigString("str-usage", "Usage: /cast spellbook <spell> [uses]");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strLearned = getConfigString("str-learned", "You have learned the %s spell!");
		strNoTarget = getConfigString("str-no-target", "You must target a bookcase to create a spellbook.");
		strCantTeach = getConfigString("str-cant-teach", "You can't create a spellbook with that spell.");
		strCantLearn = getConfigString("str-cant-learn", "You cannot learn the spell in this spellbook.");
		strLearnError = getConfigString("str-learn-error", "");
		strCantDestroy = getConfigString("str-cant-destroy", "You cannot destroy a bookcase with a spellbook.");
		strHasSpellbook = getConfigString("str-has-spellbook", "That bookcase already has a spellbook.");
		strAlreadyKnown = getConfigString("str-already-known", "You already know the %s spell.");

		loadSpellbooks();
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length < 1 || args.length > 2 || (args.length == 2 && !RegexUtil.matches(PATTERN_CAST_ARG_USAGE, args[1]))) {
				sendMessage(strUsage, player, args);
				return PostCastAction.HANDLE_NORMALLY;
			}
			if (player.isOp() && args[0].equalsIgnoreCase("reload")) {
				bookLocations = new ArrayList<>();
				bookSpells = new ArrayList<>();
				bookUses = new ArrayList<>();
				loadSpellbooks();
				player.sendMessage("Spellbook file reloaded.");
				return PostCastAction.ALREADY_HANDLED;
			}

			Spellbook spellbook = MagicSpells.getSpellbook(player);
			Spell spell = MagicSpells.getSpellByInGameName(args[0]);
			if (spellbook == null || spell == null || !spellbook.hasSpell(spell)) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!MagicSpells.getSpellbook(player).canTeach(spell)) {
				sendMessage(strCantTeach, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Block target = getTargetedBlock(player, new Power(10));
			if (target == null || !spellbookBlock.equals(target.getType())) {
				sendMessage(strNoTarget, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (bookLocations.contains(new MagicLocation(target.getLocation()))) {
				sendMessage(strHasSpellbook, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			bookLocations.add(new MagicLocation(target.getLocation()));
			bookSpells.add(spell.getInternalName());
			if (args.length == 1) bookUses.add(defaultUses);
			else bookUses.add(Integer.parseInt(args[1]));

			saveSpellbooks();
			sendMessage(formatMessage(strCastSelf, "%s", spell.getName()), player, args);
			playSpellEffects(player, target.getLocation());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (sender.isOp() && args != null && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			bookLocations = new ArrayList<>();
			bookSpells = new ArrayList<>();
			bookUses = new ArrayList<>();
			loadSpellbooks();
			sender.sendMessage("Spellbook file reloaded.");
			return true;
		}
		return false;
	}
	
	private void removeSpellbook(int index) {
		bookLocations.remove(index);
		bookSpells.remove(index);
		bookUses.remove(index);
		saveSpellbooks();
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (spellbookBlock == null) return;
		if (!event.hasBlock() || !spellbookBlock.equals(event.getClickedBlock().getType()) || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) return;
		MagicLocation loc = new MagicLocation(event.getClickedBlock().getLocation());
		if (!bookLocations.contains(loc)) return;

		event.setCancelled(true);
		Player player = event.getPlayer();
		int i = bookLocations.indexOf(loc);
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		Spell spell = MagicSpells.getSpellByInternalName(bookSpells.get(i));
		if (spellbook == null || spell == null) {
			sendMessage(strLearnError, player, MagicSpells.NULL_ARGS);
			return;
		}
		if (!spellbook.canLearn(spell)) {
			sendMessage(formatMessage(strCantLearn, "%s", spell.getName()), player, MagicSpells.NULL_ARGS);
			return;
		}
		if (spellbook.hasSpell(spell)) {
			sendMessage(formatMessage(strAlreadyKnown, "%s", spell.getName()), player, MagicSpells.NULL_ARGS);
			return;
		}
		SpellLearnEvent learnEvent = new SpellLearnEvent(spell, player, LearnSource.SPELLBOOK, event.getClickedBlock());
		EventUtil.call(learnEvent);
		if (learnEvent.isCancelled()) {
			sendMessage(formatMessage(strCantLearn, "%s", spell.getName()), player, MagicSpells.NULL_ARGS);
			return;
		}
		spellbook.addSpell(spell);
		spellbook.save();
		sendMessage(formatMessage(strLearned, "%s", spell.getName()), player, MagicSpells.NULL_ARGS);
		playSpellEffects(EffectPosition.DELAYED, player);

		int uses = bookUses.get(i);
		if (uses <= 0) return;

		uses--;
		if (uses == 0) {
			if (destroyBookcase) bookLocations.get(i).getLocation().getBlock().setType(Material.AIR);
			removeSpellbook(i);
		} else bookUses.set(i, uses);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (spellbookBlock == null || !spellbookBlock.equals(event.getBlock().getType())) return;
		MagicLocation loc = new MagicLocation(event.getBlock().getLocation());
		if (!bookLocations.contains(loc)) return;
		Player pl = event.getPlayer();
		if (pl.isOp() || Perm.ADVANCEDSPELLBOOK.has(pl)) {
			int i = bookLocations.indexOf(loc);
			removeSpellbook(i);
			return;
		}

		event.setCancelled(true);
		sendMessage(strCantDestroy, pl, MagicSpells.NULL_ARGS);
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (sender instanceof Player && !partial.contains(" ")) return tabCompleteSpellName(sender, partial);
		return null;
	}
	
	private void loadSpellbooks() {
		try {
			Scanner scanner = new Scanner(new File(MagicSpells.plugin.getDataFolder(), "books.txt"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				if (line.isEmpty()) continue;
				try {
					String[] data = line.split(":");
					MagicLocation loc = new MagicLocation(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]));
					int uses = Integer.parseInt(data[5]);
					bookLocations.add(loc);
					bookSpells.add(data[4]);
					bookUses.add(uses);
				} catch (Exception e) {
					MagicSpells.plugin.getServer().getLogger().severe("MagicSpells: Failed to load spellbook: " + line);
				}
			}
		} catch (FileNotFoundException e) {
			//DebugHandler.debugFileNotFoundException(e);
		} 
	}
	
	private void saveSpellbooks() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(MagicSpells.plugin.getDataFolder(), "books.txt"), false));
			MagicLocation loc;
			for (int i = 0; i < bookLocations.size(); i++) {
				loc = bookLocations.get(i);
				writer.write(loc.getWorld() + ':' + (int) loc.getX() + ':' + (int) loc.getY() + ':' + (int) loc.getZ() + ':');
				writer.write(bookSpells.get(i) + ':' + bookUses.get(i));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			MagicSpells.plugin.getServer().getLogger().severe("MagicSpells: Error saving spellbooks");
		}
	}
	
}
