package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class FoodSpell extends InstantSpell {

	private int food;
	private float saturation;
	private float maxSaturation;
	
	public FoodSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		food = getConfigInt("food", 4);
		saturation = getConfigFloat("saturation", 2.5F);
		maxSaturation = getConfigFloat("max-saturation", 0F);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			int f = player.getFoodLevel() + food;
			if (f > 20) f = 20;
			player.setFoodLevel(f);
			
			float s = player.getSaturation() + saturation;
			if (maxSaturation > 0 && saturation > maxSaturation) saturation = maxSaturation;
			player.setSaturation(s);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
