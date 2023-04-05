package com.nisovin.magicspells.spells.command;

import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;

// TODO this should not be hardcoded to use a book
public class TomeSpell extends CommandSpell {

	private static final Pattern INT_PATTERN = Pattern.compile("^[0-9]+$");
	
	private boolean consumeBook;
	private boolean allowOverwrite;
	private boolean requireTeachPerm;
	private boolean cancelReadOnLearn;

	private int maxUses;
	private int defaultUses;

	private String strUsage;
	private String strNoBook;
	private String strNoSpell;
	private String strLearned;
	private String strCantLearn;
	private String strCantTeach;
	private String strAlreadyKnown;
	private String strAlreadyHasSpell;

	public TomeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		consumeBook = getConfigBoolean("consume-book", false);
		allowOverwrite = getConfigBoolean("allow-overwrite", false);
		requireTeachPerm = getConfigBoolean("require-teach-perm", true);
		cancelReadOnLearn = getConfigBoolean("cancel-read-on-learn", true);

		maxUses = getConfigInt("max-uses", 5);
		defaultUses = getConfigInt("default-uses", -1);

		strUsage = getConfigString("str-usage", "Usage: While holding a written book, /cast " + name + " <spell> [uses]");
		strNoBook = getConfigString("str-no-book", "You must be holding a written book.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell with that name.");
		strLearned = getConfigString("str-learned", "You have learned the %s spell.");
		strCantLearn = getConfigString("str-cant-learn", "You cannot learn the spell in this tome.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a tome with that spell.");
		strAlreadyKnown = getConfigString("str-already-known", "You already know the %s spell.");
		strAlreadyHasSpell = getConfigString("str-already-has-spell", "That book already contains a spell.");
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			Spell spell;
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Spellbook spellbook = MagicSpells.getSpellbook(player);
			spell = MagicSpells.getSpellByInGameName(args[0]);
			if (spell == null || spellbook == null || !spellbook.hasSpell(spell)) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (requireTeachPerm && !MagicSpells.getSpellbook(player).canTeach(spell)) {
				sendMessage(strCantTeach, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			ItemStack item = player.getEquipment().getItemInMainHand();
			if (item.getType() != Material.WRITTEN_BOOK) {
				sendMessage(strNoBook, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!allowOverwrite && getSpellDataFromTome(item) != null) {
				sendMessage(strAlreadyHasSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			int uses = defaultUses;
			if (args.length > 1 && RegexUtil.matches(INT_PATTERN, args[1])) uses = Integer.parseInt(args[1]);
			item = createTome(spell, uses, item);
			player.getEquipment().setItemInMainHand(item);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
	private String getSpellDataFromTome(ItemStack item) {
		String loreData = Util.getLoreData(item);
		if (loreData != null && loreData.startsWith(internalName + ':')) return loreData.replace(internalName + ':', "");
		return null;
	}

	public ItemStack createTome(Spell spell, int uses, ItemStack item) {
		if (maxUses > 0 && uses > maxUses) uses = maxUses;
		else if (uses < 0) uses = defaultUses;
		if (item == null) {
			item = new ItemStack(Material.WRITTEN_BOOK, 1);
			BookMeta bookMeta = (BookMeta)item.getItemMeta();
			bookMeta.setTitle(getName() + ": " + spell.getName());
			item.setItemMeta(bookMeta);
		}
		Util.setLoreData(item, internalName + ':' + spell.getInternalName() + (uses > 0 ? "," + uses : ""));
		return item;
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!event.hasItem()) return;
		ItemStack item = event.getItem();
		if (item.getType() != Material.WRITTEN_BOOK) return;
		
		String spellData = getSpellDataFromTome(item);
		if (spellData == null || spellData.isEmpty()) return;
		
		String[] data = spellData.split(",");
		Spell spell = MagicSpells.getSpellByInternalName(data[0]);
		int uses = -1;
		if (data.length > 1) uses = Integer.parseInt(data[1]);
		Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
		if (spell == null) return;
		if (spellbook == null) return;
		
		if (spellbook.hasSpell(spell)) {
			sendMessage(formatMessage(strAlreadyKnown, "%s", spell.getName()), event.getPlayer(), MagicSpells.NULL_ARGS);
			return;
		}
		if (!spellbook.canLearn(spell)) {
			sendMessage(formatMessage(strCantLearn, "%s", spell.getName()), event.getPlayer(), MagicSpells.NULL_ARGS);
			return;
		}
		SpellLearnEvent learnEvent = new SpellLearnEvent(spell, event.getPlayer(), LearnSource.TOME, event.getPlayer().getEquipment().getItemInMainHand());
		EventUtil.call(learnEvent);
		if (learnEvent.isCancelled()) {
			sendMessage(formatMessage(strCantLearn, "%s", spell.getName()), event.getPlayer(), MagicSpells.NULL_ARGS);
			return;
		}
		spellbook.addSpell(spell);
		spellbook.save();
		sendMessage(formatMessage(strLearned, "%s", spell.getName()), event.getPlayer(), MagicSpells.NULL_ARGS);
		if (cancelReadOnLearn) event.setCancelled(true);

		if (uses > 0) {
			uses--;
			if (uses > 0) Util.setLoreData(item, internalName + ':' + data[0] + ',' + uses);
			else Util.removeLoreData(item);

		}
		if (uses <= 0 && consumeBook) event.getPlayer().getEquipment().setItemInMainHand(null);
		playSpellEffects(EffectPosition.DELAYED, event.getPlayer());
	}

}
