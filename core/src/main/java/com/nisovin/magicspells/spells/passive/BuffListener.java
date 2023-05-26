package com.nisovin.magicspells.spells.passive;

import java.util.List;
import java.util.ArrayList;

import com.nisovin.magicspells.power.Power;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellForgetEvent;

// No trigger variable currently used
public class BuffListener extends PassiveListener {

	private List<PassiveSpell> spells = new ArrayList<>();

	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		spells.add(spell);
		for (Subspell s : spell.getActivatedSpells()) {
			if (!(s.getSpell() instanceof BuffSpell)) continue;
			BuffSpell buff = (BuffSpell) s.getSpell();
			buff.setAsEverlasting();
		}
	}

	@Override
	public void initialize() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			on(player);
		}
	}

	@OverridePriority
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		on(event.getPlayer());
	}

	@OverridePriority
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		off(event.getPlayer());
	}

	@OverridePriority
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		off(event.getEntity());
	}

	@OverridePriority
	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		MagicSpells.scheduleDelayedTask(() -> on(event.getPlayer()), 1);
	}

	@OverridePriority
	@EventHandler
	public void onSpellLearn(final SpellLearnEvent event) {
		if (event.getSpell() instanceof PassiveSpell && spells.contains(event.getSpell())) {
			MagicSpells.scheduleDelayedTask(() -> on(event.getLearner(), (PassiveSpell) event.getSpell()), 1);
		}
	}

	@OverridePriority
	@EventHandler
	public void onSpellForget(SpellForgetEvent event) {
		if (event.getSpell() instanceof PassiveSpell && spells.contains(event.getSpell())) {
			off(event.getForgetter(), (PassiveSpell) event.getSpell());
		}
	}

	private void on(Player player) {
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		for (PassiveSpell spell : spells) {
			if (spellbook.hasSpell(spell)) on(player, spell);
		}
	}

	private void on(Player player, PassiveSpell spell) {
		for (Subspell s : spell.getActivatedSpells()) {
			if (!(s.getSpell() instanceof BuffSpell)) continue;
			BuffSpell buff = (BuffSpell) s.getSpell();
			if (buff.isActive(player)) continue;
			buff.castAtEntity(player, player, new Power(1));
		}
	}

	private void off(Player player) {
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		for (PassiveSpell spell : spells) {
			if (spellbook.hasSpell(spell)) off(player, spell);
		}
	}

	private void off(Player player, PassiveSpell spell) {
		for (Subspell s : spell.getActivatedSpells()) {
			if (!(s.getSpell() instanceof BuffSpell)) continue;
			BuffSpell buff = (BuffSpell) s.getSpell();
			if (!buff.isActive(player)) continue;
			buff.turnOff(player);
		}
	}

}
