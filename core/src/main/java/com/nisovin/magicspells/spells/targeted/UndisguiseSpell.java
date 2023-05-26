package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.IDisguiseManager;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class UndisguiseSpell extends TargetedSpell implements TargetedEntitySpell {

	private IDisguiseManager manager;
	
	public UndisguiseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		manager = DisguiseSpell.getDisguiseManager();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, Power power, String[] args) {
		if (manager == null) return PostCastAction.ALREADY_HANDLED;
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> target = getTargetPlayer(caster, power);
			if (target == null) return noTarget(caster);

			undisguise(caster, target.getTarget());
			sendMessages(caster, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		return target instanceof Player && undisguise(caster, (Player) target);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		return target instanceof Player && undisguise(null, (Player) target);
	}

	private boolean undisguise(LivingEntity caster, Player player) {
		if (manager == null) return false;
		manager.removeDisguise(player);
		if (caster != null) playSpellEffects(caster, player);
		else playSpellEffects(EffectPosition.TARGET, player);
		return true;
	}

}
