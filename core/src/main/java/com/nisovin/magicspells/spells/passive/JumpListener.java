package com.nisovin.magicspells.spells.passive;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;

// No trigger variable is currently used
public class JumpListener extends PassiveListener {

	List<PassiveSpell> spells = new ArrayList<>();

	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		spells.add(spell);
	}

	@OverridePriority
	@EventHandler
	public void onJoin(PlayerStatisticIncrementEvent event) {
		Player player = event.getPlayer();
		if (event.getStatistic() != Statistic.JUMP) return;
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		spells.stream().filter(spellbook::hasSpell).forEachOrdered(spell -> spell.activate(player));
	}

}
