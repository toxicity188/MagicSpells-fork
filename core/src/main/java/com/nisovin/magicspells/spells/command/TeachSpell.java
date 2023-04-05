package com.nisovin.magicspells.spells.command;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;

public class TeachSpell extends CommandSpell {

	private boolean requireKnownSpell;

	private String strUsage;
	private String strNoSpell;
	private String strNoTarget;
	private String strCantTeach;
	private String strCantLearn;
	private String strCastTarget;
	private String strAlreadyKnown;

	public TeachSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireKnownSpell = getConfigBoolean("require-known-spell", true);

		strUsage = getConfigString("str-usage", "Usage: /cast teach <target> <spell>");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strNoTarget = getConfigString("str-no-target", "No such player.");
		strCantTeach = getConfigString("str-cant-teach", "You can't teach that spell.");
		strCantLearn = getConfigString("str-cant-learn", "That person cannot learn that spell.");
		strCastTarget = getConfigString("str-cast-target", "%a has taught you the %s spell.");
		strAlreadyKnown = getConfigString("str-already-known", "That person already knows that spell.");
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length != 2) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			List<Player> players = MagicSpells.plugin.getServer().matchPlayer(args[0]);
			if (players.size() != 1) {
				sendMessage(strNoTarget, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			Spell spell = MagicSpells.getSpellByInGameName(args[1]);
			Player target = players.get(0);
			if (spell == null) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			if (spellbook == null || (!spellbook.hasSpell(spell) && requireKnownSpell)) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!spellbook.canTeach(spell)) {
				sendMessage(strCantTeach, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			Spellbook targetSpellbook = MagicSpells.getSpellbook(target);
			if (targetSpellbook == null || !targetSpellbook.canLearn(spell)) {
				sendMessage(strCantLearn, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (targetSpellbook.hasSpell(spell)) {
				sendMessage(strAlreadyKnown, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			boolean cancelled = callEvent(spell, target, player);
			if (cancelled) {
				sendMessage(strCantLearn, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			targetSpellbook.addSpell(spell);
			targetSpellbook.save();
			sendMessage(formatMessage(strCastTarget, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", target.getDisplayName()), target, args);
			sendMessage(formatMessage(strCastSelf, "%a", player.getDisplayName(), "%s", spell.getName(), "%t", target.getDisplayName()), player, args);
			playSpellEffects(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args == null || args.length != 2) {
			sender.sendMessage(strUsage);
			return true;
		}
		List<Player> players = MagicSpells.plugin.getServer().matchPlayer(args[0]);
		if (players.size() != 1) {
			sender.sendMessage(strNoTarget);
			return true;
		}
		Spell spell = MagicSpells.getSpellByInGameName(args[1]);
		if (spell == null) {
			sender.sendMessage(strNoSpell);
			return true;
		}
		Spellbook targetSpellbook = MagicSpells.getSpellbook(players.get(0));
		if (targetSpellbook == null || !targetSpellbook.canLearn(spell)) {
			sender.sendMessage(strCantLearn);
			return true;
		}
		if (targetSpellbook.hasSpell(spell)) {
			sender.sendMessage(strAlreadyKnown);
			return true;
		}
		boolean cancelled = callEvent(spell, players.get(0), sender);
		if (cancelled) {
			sender.sendMessage(strCantLearn);
			return true;
		}
		targetSpellbook.addSpell(spell);
		targetSpellbook.save();
		sendMessage(formatMessage(strCastTarget, "%a", getConsoleName(), "%s", spell.getName(), "%t", players.get(0).getDisplayName()), players.get(0), args);
		sender.sendMessage(formatMessage(strCastSelf, "%a", getConsoleName(), "%s", spell.getName(), "%t", players.get(0).getDisplayName()));
		return true;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String[] args = Util.splitParams(partial);
		if (args.length == 1) return tabCompletePlayerName(sender, args[0]);
		if (args.length == 2) return tabCompleteSpellName(sender, args[1]);
		return null;
	}
	
	private boolean callEvent(Spell spell, Player learner, Object teacher) {
		SpellLearnEvent event = new SpellLearnEvent(spell, learner, LearnSource.TEACH, teacher);
		EventUtil.call(event);
		return event.isCancelled();
	}

}
