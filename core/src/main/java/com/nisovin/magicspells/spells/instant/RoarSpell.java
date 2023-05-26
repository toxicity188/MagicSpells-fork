package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RoarSpell extends InstantSpell {

	private float radius;

	private String strNoTarget;

	private boolean cancelIfNoTargets;

	public RoarSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigFloat("radius", 8F);

		strNoTarget = getConfigString("str-no-target", "No targets found.");

		cancelIfNoTargets = getConfigBoolean("cancel-if-no-targets", true);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int count = 0;
			List<Entity> entities = livingEntity.getNearbyEntities(radius, radius, radius);
			for (Entity entity : entities) {
				if (!(entity instanceof LivingEntity)) continue;
				if (entity instanceof Player) continue;
				if (!validTargetList.canTarget(livingEntity, entity)) continue;
				MagicSpells.getVolatileCodeHandler().setTarget((LivingEntity) entity, livingEntity);
				playSpellEffectsTrail(livingEntity.getLocation(), entity.getLocation());
				playSpellEffects(EffectPosition.TARGET, entity);
				count++;
			}
			if (cancelIfNoTargets && count == 0) {
				sendMessage(strNoTarget, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			playSpellEffects(EffectPosition.CASTER, livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
