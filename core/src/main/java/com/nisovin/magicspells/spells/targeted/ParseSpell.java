package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ParseSpell extends TargetedSpell implements TargetedEntitySpell {

	private int op;

	private String parseTo;
	private String operation;
	private String firstVariable;
	private String expectedValue;
	private String secondVariable;
	private String parseToVariable;
	private String variableToParse;

	public ParseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		parseTo = getConfigString("parse-to", "");
		operation = getConfigString("operation", "normal");
		firstVariable = getConfigString("first-variable", "");
		expectedValue = getConfigString("expected-value", "");
		secondVariable = getConfigString("second-variable", "");
		parseToVariable = getConfigString("parse-to-variable", "");
		variableToParse = getConfigString("variable-to-parse", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		op = 0;
		if (operation.contains("translate") || operation.contains("normal")) {
			if (expectedValue.isEmpty()) {
				MagicSpells.error("ParseSpell '" + internalName + "' has an invalid expected-value defined!");
				return;
			}
			if (parseToVariable.isEmpty()) {
				MagicSpells.error("ParseSpell '" + internalName + "' has an invalid parse-to-variable defined!");
				return;
			}
			if (variableToParse.isEmpty() || MagicSpells.getVariableManager().getVariable(variableToParse) == null) {
				MagicSpells.error("ParseSpell '" + internalName + "' has an invalid variable-to-parse defined!");
				return;
			}
			op = 1;
		}

		if (operation.contains("difference")) {
			if (firstVariable.isEmpty() || MagicSpells.getVariableManager().getVariable(firstVariable) == null) {
				MagicSpells.error("ParseSpell '" + internalName + "' has an invalid first-variable defined!");
				return;
			}
			if (secondVariable.isEmpty() || MagicSpells.getVariableManager().getVariable(secondVariable) == null) {
				MagicSpells.error("ParseSpell '" + internalName + "' has an invalid second-variable defined!");
				return;
			}
			op = 2;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(livingEntity);

			parse(target);
			playSpellEffects(livingEntity, target);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		playSpellEffects(caster, target);
		parse(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		playSpellEffects(EffectPosition.TARGET, target);
		parse(target);
		return true;
	}

	private void parse(LivingEntity target) {
		if (!(target instanceof Player)) return;
		if (op == 0) return;

		if (op == 1) {
			String receivedValue = MagicSpells.getVariableManager().getStringValue(variableToParse, (Player) target);
			if (!receivedValue.equalsIgnoreCase(expectedValue) && !expectedValue.contains("any")) return;
			MagicSpells.getVariableManager().set(parseToVariable, (Player) target, parseTo);
		} else if (op == 2) {
			double primary = MagicSpells.getVariableManager().getValue(firstVariable, (Player) target);
			double secondary = MagicSpells.getVariableManager().getValue(secondVariable, (Player) target);
			double diff = Math.abs(primary - secondary);
			MagicSpells.getVariableManager().set(parseToVariable, (Player) target, diff);
		}
	}

}
