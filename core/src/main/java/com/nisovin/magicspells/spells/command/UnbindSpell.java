package com.nisovin.magicspells.spells.command;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class UnbindSpell extends CommandSpell {

	private List<String> allowedSpellsNames;
	private Set<Spell> allowedSpells = null;

	private String strUsage;
	private String strNoSpell;
	private String strNotBound;
	private String strUnbindAll;
	private String strCantUnbind;
	private String strCantBindSpell;

	public UnbindSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		allowedSpellsNames = getConfigStringList("allowed-spells", null);
		if (allowedSpellsNames != null && !allowedSpellsNames.isEmpty()) {
			allowedSpells = new HashSet<>();
			for (String n: allowedSpellsNames) {
				Spell s = MagicSpells.getSpellByInternalName(n);
				if (s != null) allowedSpells.add(s);
				else MagicSpells.plugin.getLogger().warning("Invalid spell defined: " + n);
			}
		}

		strUsage = getConfigString("str-usage", "You must specify a spell name.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strNotBound = getConfigString("str-not-bound", "That spell is not bound to that item.");
		strUnbindAll = getConfigString("str-unbind-all", "All spells from your item were cleared.");
		strCantUnbind = getConfigString("str-cant-unbind", "You cannot unbind this spell");
		strCantBindSpell = getConfigString("str-cant-bind-spell", "That spell cannot be bound to an item.");
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			CastItem item = new CastItem(player.getEquipment().getItemInMainHand());
			Spellbook spellbook = MagicSpells.getSpellbook(player);

			if (args[0] != null && args[0].equalsIgnoreCase("*")) {
				List<Spell> spells = new ArrayList<>();

				for (CastItem i : spellbook.getItemSpells().keySet()) {
					if (!i.equals(item)) continue;
					spells.addAll(spellbook.getItemSpells().get(i));
				}

				for (Spell s : spells) {
					spellbook.removeCastItem(s, item);
				}

				spellbook.save();
				sendMessage(strUnbindAll, player, args);
				playSpellEffects(EffectPosition.CASTER, player);
				return PostCastAction.NO_MESSAGES;
			}

			Spell spell = MagicSpells.getSpellByInGameName(Util.arrayJoin(args, ' '));
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
				sendMessage(strCantUnbind, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			boolean removed = spellbook.removeCastItem(spell, item);
			if (!removed) {
				sendMessage(strNotBound, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			spellbook.save();
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
