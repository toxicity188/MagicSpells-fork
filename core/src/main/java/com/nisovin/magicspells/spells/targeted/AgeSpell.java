package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class AgeSpell extends TargetedSpell implements TargetedEntitySpell {

	private int rawAge;
	private boolean setMaturity;
	private boolean applyAgeLock;

	public AgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		rawAge = getConfigInt("age", 0);
		setMaturity = getConfigBoolean("set-maturity", true);
		applyAgeLock = getConfigBoolean("apply-age-lock", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetEntityInfo = getTargetedEntity(livingEntity, power);
			if (targetEntityInfo == null || targetEntityInfo.getTarget() == null) return noTarget(livingEntity);
			if (!(targetEntityInfo.getTarget() instanceof Ageable)) return noTarget(livingEntity);

			Ageable a = (Ageable) targetEntityInfo.getTarget();
			applyAgeChanges(a);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Ageable)) return false;
		applyAgeChanges((Ageable) target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(null, target, power);
	}

	private void applyAgeChanges(Ageable a) {
		if (setMaturity) a.setAge(rawAge);
		if (applyAgeLock) a.setAgeLock(true);
	}

}
