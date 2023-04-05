package com.nisovin.magicspells.spells.targeted;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.data.DataEntity;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class DataSpell extends TargetedSpell implements TargetedEntitySpell {

	private String variableName;
	private Function<? super LivingEntity, String> dataElement;
	
	public DataSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		variableName = getConfigString("variable-name", "");
		dataElement = DataEntity.getDataFunction(getConfigString("data-element", "uuid"));
	}
	
	@Override
	public void initialize() {
		if (variableName.isEmpty() || MagicSpells.getVariableManager().getVariable(variableName) == null) {
			MagicSpells.error("DataSpell '" + internalName + "' has an invalid variable-name defined!");
			return;
		}

		if (dataElement == null) MagicSpells.error("DataSpell '" + internalName + "' has an invalid option defined for data-element!");

	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
			if (targetInfo == null) return noTarget(player);
			LivingEntity target = targetInfo.getTarget();
			if (target == null) return noTarget(player);

			playSpellEffects(player, target);
			String value = dataElement.apply(target);
			MagicSpells.getVariableManager().set(variableName, player, value);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(caster instanceof Player)) return false;
		playSpellEffects(caster, target);
		String value = dataElement.apply(target);
		MagicSpells.getVariableManager().set(variableName, (Player) caster, value);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
