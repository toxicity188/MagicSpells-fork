package com.nisovin.magicspells.spells.instant;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class CastAtMarkSpell extends InstantSpell {

	private String strNoMark;
	private String markSpellName;
	private String spellToCastName;

	private MarkSpell markSpell;
	private Subspell spellToCast;

	private boolean initialized = false;

	public CastAtMarkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strNoMark = getConfigString("str-no-mark", "You do not have a mark specified");
		markSpellName = getConfigString("mark-spell", "");
		spellToCastName = getConfigString("spell", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (initialized) return;

		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell == null || !(spell instanceof MarkSpell)) {
			MagicSpells.error("CastAtMarkSpell '" + internalName + "' has an invalid mark-spell defined!");
			return;
		}
		
		markSpell = (MarkSpell) spell;
		
		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process() || !spellToCast.isTargetedLocationSpell()) {
			MagicSpells.error("CastAtMarkSpell '" + internalName + "' has an invalid spell defined!");
			return;
		}
		
		initialized = true;
	}

	@Override
	public void turnOff() {
		super.turnOff();

		markSpell = null;
		spellToCast = null;
		initialized = false;
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (!initialized) return PostCastAction.HANDLE_NORMALLY;
		if (state == SpellCastState.NORMAL) {
			Location effectiveMark = markSpell.getEffectiveMark(livingEntity);
			if (effectiveMark == null) {
				sendMessage(livingEntity, strNoMark);
				return PostCastAction.HANDLE_NORMALLY;
			}
			spellToCast.castAtLocation(livingEntity, effectiveMark, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
