package com.nisovin.magicspells.castmodifiers;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public interface IModifier {

	boolean apply(SpellCastEvent event);
	boolean apply(ManaChangeEvent event);
	boolean apply(SpellTargetEvent event);
	boolean apply(SpellTargetLocationEvent event);
	boolean apply(MagicSpellsGenericPlayerEvent event);
	boolean check(LivingEntity livingEntity);
	boolean check(LivingEntity livingEntity, LivingEntity entity);

}
