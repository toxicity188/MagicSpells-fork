package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.TargetBooleanState;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class FlySpell extends TargetedSpell implements TargetedEntitySpell {
	
	private TargetBooleanState targetBooleanState;
	
	public FlySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		targetBooleanState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(livingEntity);

			target.setFlying(targetBooleanState.getBooleanState(target.isFlying()));
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		if (!(target instanceof Player)) return false;
		((Player) target).setFlying(targetBooleanState.getBooleanState(((Player) target).isFlying()));
		return true;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		if (!(target instanceof Player)) return false;
		((Player) target).setFlying(targetBooleanState.getBooleanState(((Player) target).isFlying()));
		return true;
	}
	
}
