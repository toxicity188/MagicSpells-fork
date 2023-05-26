package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class CraftSpell extends InstantSpell {

	public CraftSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			((Player) livingEntity).openWorkbench(null, true);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
