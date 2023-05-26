package com.nisovin.magicspells.spells.command;

import com.nisovin.magicspells.power.Power;

import java.util.List;
import java.util.Collection;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.PlayerNameUtils;

// Advanced perm is for listing other player's spells

public class SublistSpell extends CommandSpell {

	private List<String> spellsToHide;
	private List<String> spellsToShow;

	private int lineLength = 60;

	private boolean reloadGrantedSpells;
	private boolean onlyShowCastableSpells;

	private String strPrefix;
	private String strNoSpells;

	public SublistSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellsToHide = getConfigStringList("spells-to-hide", null);
		spellsToShow = getConfigStringList("spells-to-show", null);

		reloadGrantedSpells = getConfigBoolean("reload-granted-spells", true);
		onlyShowCastableSpells = getConfigBoolean("only-show-castable-spells", false);

		strPrefix = getConfigString("str-prefix", "Known spells:");
		strNoSpells = getConfigString("str-no-spells", "You do not know any spells.");
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			String extra = "";
			if (args != null && args.length > 0 && spellbook.hasAdvancedPerm("list")) {
				Player p = PlayerNameUtils.getPlayer(args[0]);
				if (p != null) {
					spellbook = MagicSpells.getSpellbook(p);
					extra = '(' + p.getDisplayName() + ") ";
				}
			}
			if (spellbook != null && reloadGrantedSpells) spellbook.addGrantedSpells();
			if (spellbook == null || spellbook.getSpells().isEmpty()) {
				sendMessage(strNoSpells, player, args);
				return PostCastAction.HANDLE_NORMALLY;
			}

			String s = "";
			for (Spell spell : spellbook.getSpells()) {
				if (!spell.isHelperSpell() && (!onlyShowCastableSpells || spellbook.canCast(spell))
						&& !(spellsToHide != null && spellsToHide.contains(spell.getInternalName()))
						&& (spellsToShow == null || spellsToShow.contains(spell.getInternalName()))) {
					if (s.isEmpty()) s = spell.getName();
					else s += ", " + spell.getName();
				}
			}
			s = strPrefix + ' ' + extra + s;
			while (s.length() > lineLength) {
				int i = s.substring(0, lineLength).lastIndexOf(' ');
				sendMessage(s.substring(0, i), player, args);
				s = s.substring(i + 1);
			}
			if (!s.isEmpty()) sendMessage(s, player, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		StringBuilder s = new StringBuilder();

		Collection<Spell> spells = MagicSpells.spells();
		if (args != null && args.length > 0) {
			Player p = PlayerNameUtils.getPlayer(args[0]);
			if (p == null) {
				sender.sendMessage("No such player.");
				return true;
			}
			spells = MagicSpells.getSpellbook(p).getSpells();
			s.append(p.getName()).append("'s spells: ");
		} else s.append("All spells: ");

		for (Spell spell : spells) {
			s.append(spell.getName());
			s.append(' ');
		}

		sender.sendMessage(s.toString());
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (sender instanceof ConsoleCommandSender && !partial.contains(" ")) return tabCompletePlayerName(sender, partial);
		return null;
	}

}
