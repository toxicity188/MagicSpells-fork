package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class SwitchSpell extends TargetedSpell implements TargetedEntitySpell {

	private int switchBack;
	
	public SwitchSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		switchBack = getConfigInt("switch-back", 0);
	}

	@Override
	public PostCastAction castSpell(LivingEntity player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) return noTarget(player);
			
			playSpellEffects(player, target.getTarget());
			switchPlaces(player, target.getTarget());
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		switchPlaces(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private void switchPlaces(LivingEntity player, final LivingEntity target) {
		Location targetLoc = target.getLocation();
		Location casterLoc = player.getLocation();
		player.teleport(targetLoc);
		target.teleport(casterLoc);

		if (switchBack <= 0) return;

		MagicSpells.scheduleDelayedTask(() -> {
			if (player.isDead() || target.isDead()) return;
			Location targetLoc1 = target.getLocation();
			Location casterLoc1 = player.getLocation();
			player.teleport(targetLoc1);
			target.teleport(casterLoc1);
		}, switchBack);
	}

}
