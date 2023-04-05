package com.nisovin.magicspells.spells;

import java.util.List;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PermissionSpell extends InstantSpell {

	private int duration;
	
	private List<String> permissionNodes;
	
	public PermissionSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		duration = getConfigInt("duration", 0);
		permissionNodes = getConfigStringList("permission-nodes", null);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && duration > 0 && permissionNodes != null) {
			for (String node : permissionNodes) {
				livingEntity.addAttachment(MagicSpells.plugin, node, true, duration);
			}
			playSpellEffects(EffectPosition.CASTER, livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
