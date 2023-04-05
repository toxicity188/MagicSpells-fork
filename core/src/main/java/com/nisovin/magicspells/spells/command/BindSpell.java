package com.nisovin.magicspells.spells.command;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class BindSpell extends CommandSpell {
	
	private Set<CastItem> bindableItems;

	private Set<Spell> allowedSpells;

	private boolean allowBindToFist;

	private String strUsage;
	private String strNoSpell;
	private String strCantBindItem;
	private String strCantBindSpell;
	private String strSpellCantBind;

	public BindSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> bindables = getConfigStringList("bindable-items", null);
		if (bindables != null) {
			bindableItems = new HashSet<>();
			for (String s : bindables) {
				bindableItems.add(new CastItem(s));
			}
		}

		List<String> allowedSpellNames = getConfigStringList("allowed-spells", null);
		if (allowedSpellNames != null && !allowedSpellNames.isEmpty()) {
			allowedSpells = new HashSet<>();
			for (String name: allowedSpellNames) {
				Spell s = MagicSpells.getSpellByInternalName(name);
				if (s != null) allowedSpells.add(s);
				else MagicSpells.plugin.getLogger().warning("Invalid spell listed: " + name);

			}
		}

		allowBindToFist = getConfigBoolean("allow-bind-to-fist", false);

		strUsage = getConfigString("str-usage", "You must specify a spell name and hold an item in your hand.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strCantBindItem = getConfigString("str-cant-bind-item", "That spell cannot be bound to that item.");
		strCantBindSpell = getConfigString("str-cant-bind-spell", "That spell cannot be bound to an item.");
		strSpellCantBind = getConfigString("str-spell-cant-bind", "That spell cannot be bound like this.");
	}
	
	// DEBUG INFO: level 3, trying to bind spell internalname to cast item castitemstring
	// DEBUG INFO: level 3, performing bind
	// DEBUG INFO: level 3, bind successful
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Spell spell = MagicSpells.getSpellByInGameName(Util.arrayJoin(args, ' '));
			Spellbook spellbook = MagicSpells.getSpellbook(player);

			if (spell == null || spellbook == null) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!spellbook.hasSpell(spell)) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!spell.canCastWithItem()) {
				sendMessage(strCantBindSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (allowedSpells != null && !allowedSpells.contains(spell)) {
				sendMessage(strSpellCantBind, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			CastItem castItem = new CastItem(player.getEquipment().getItemInMainHand());
			MagicSpells.debug(3, "Trying to bind spell '" + spell.getInternalName() + "' to cast item " + castItem.toString() + "...");

			if (BlockUtils.isAir(castItem.getItemType()) && !allowBindToFist) {
				sendMessage(strCantBindItem, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (bindableItems != null && !bindableItems.contains(castItem)) {
				sendMessage(strCantBindItem, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!spell.canBind(castItem)) {
				String msg = spell.getCantBindError();
				if (msg == null) msg = strCantBindItem;
				sendMessage(msg, player, args);
				return PostCastAction.NO_MESSAGES;
			}

			MagicSpells.debug(3, "    Performing bind...");
			spellbook.addCastItem(spell, castItem);
			spellbook.save();
			MagicSpells.debug(3, "    Bind successful.");
			sendMessage(formatMessage(strCastSelf, "%s", spell.getName()), player, args);
			playSpellEffects(EffectPosition.CASTER, player);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (sender instanceof Player && !partial.contains(" ")) return tabCompleteSpellName(sender, partial);
		return null;
	}

}
