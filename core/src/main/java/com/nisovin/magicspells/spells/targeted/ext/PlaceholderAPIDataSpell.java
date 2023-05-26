package com.nisovin.magicspells.spells.targeted.ext;

import com.nisovin.magicspells.power.Power;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;

import me.clip.placeholderapi.PlaceholderAPI;

// NOTE: PLACEHOLDERAPI IS REQUIRED FOR THIS
public class PlaceholderAPIDataSpell extends TargetedSpell {
	
	private String variableName;
	private String placeholderAPITemplate;
	
	public PlaceholderAPIDataSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		variableName = getConfigString("variable-name", null);
		placeholderAPITemplate = getConfigString("placeholderapi-template", "An admin forgot to set placeholderapi-template");
	}
	
	@Override
	public void initialize() {
		if (variableName == null) {
			MagicSpells.error("PlaceholderAPIDataSpell '" + internalName + "' has an invalid variable-name defined!");
			MagicSpells.error("In most cases, this should be set to the name of a string variable, but non string variables may work depending on values.");
			return;
		}
		
		if (placeholderAPITemplate == null) {
			MagicSpells.error("PlaceholderAPIDataSpell '" + internalName + "' has an invalid placeholderapi-template defined!");
			MagicSpells.error("This was probably because you put something similar to \"placeholderapi-template\" and did not specify a value.");
		}
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(caster, power);
			if (targetInfo == null) return noTarget(caster);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(caster);
			
			String value = PlaceholderAPI.setPlaceholders(target, placeholderAPITemplate);
			MagicSpells.getVariableManager().set(variableName, (Player) caster, value);
			playSpellEffects(caster, target);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
}
