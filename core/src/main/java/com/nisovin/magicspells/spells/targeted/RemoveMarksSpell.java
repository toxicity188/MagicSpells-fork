package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.UUID;
import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.instant.MarkSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class RemoveMarksSpell extends TargetedSpell implements TargetedLocationSpell {

	private float radius;

	private boolean pointBlank;

	private MarkSpell markSpell;
	private String markSpellName;

	public RemoveMarksSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigFloat("radius", 10F);

		pointBlank = getConfigBoolean("point-blank", false);

		markSpellName = getConfigString("mark-spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell instanceof MarkSpell) {
			markSpell = (MarkSpell) spell;
			return;
		}

		MagicSpells.error("RemoveMarksSpell '" + internalName + "' has an invalid mark-spell defined!");
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			if (pointBlank) loc = livingEntity.getLocation();
			else {
				Block b = getTargetedBlock(livingEntity, power);
				if (b != null && !BlockUtils.isAir(b.getType())) loc = b.getLocation();
			}
			if (loc == null) return noTarget(livingEntity);
			removeMarks(livingEntity, loc, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		removeMarks(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		removeMarks(null, target, power);
		return true;
	}

	private void removeMarks(LivingEntity caster, Location loc, Power power) {
		float rad = radius * power.floatValue();
		float radSq = rad * rad;

		Map<UUID, MagicLocation> marks = markSpell.getMarks();
		Iterator<UUID> iter = marks.keySet().iterator();
		World locWorld = loc.getWorld();

		while (iter.hasNext()) {
			MagicLocation l = marks.get(iter.next());
			if (!l.getWorld().equals(locWorld.getName())) continue;
			if (l.getLocation().distanceSquared(loc) < radSq) iter.remove();
		}

		markSpell.setMarks(marks);
		playSpellEffects(EffectPosition.TARGET, loc);
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
	}
	
}
