package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.TargetBooleanState;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class EntitySilenceSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private TargetBooleanState targetBooleanState;
	
	public EntitySilenceSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		targetBooleanState = TargetBooleanState.getFromName(getConfigString("target-state", "toggle"));
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);

			LivingEntity target = targetInfo.getTarget();
			if (target == null) return noTarget(livingEntity);
			target.setSilent(targetBooleanState.getBooleanState(target.isSilent()));
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		target.setSilent(targetBooleanState.getBooleanState(target.isSilent()));
		return true;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		target.setSilent(targetBooleanState.getBooleanState(target.isSilent()));
		return true;
	}
	
}
