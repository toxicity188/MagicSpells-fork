package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class EnderchestSpell extends InstantSpell implements TargetedEntitySpell {

	public EnderchestSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args != null && args.length == 1 && player.hasPermission("magicspells.advanced." + internalName)) {
				Player target = PlayerNameUtils.getPlayer(args[0]);
				if (target == null) {
					player.sendMessage("Invalid player target");
					return PostCastAction.ALREADY_HANDLED;
				}
				player.openInventory(target.getEnderChest());
			} else player.openInventory(player.getEnderChest());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		if (!(caster instanceof Player)) return false;
		((Player) caster).openInventory(((Player) target).getEnderChest());
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
