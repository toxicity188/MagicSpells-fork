package com.nisovin.magicspells.spells.targeted;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class RotateSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private Random random;

	private int rotationYaw;
	private int rotationPitch;

	private boolean faceTarget;
	private boolean faceCaster;
	private boolean affectPitch;
	private boolean randomAngle;
	private boolean mimicDirection;

	public RotateSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		random = new Random();

		rotationYaw = getConfigInt("rotation", 10);
		rotationPitch = getConfigInt("rotation-pitch", 0);

		faceTarget = getConfigBoolean("face-target", false);
		faceCaster = getConfigBoolean("face-caster", false);
		affectPitch = getConfigBoolean("affect-pitch", false);
		randomAngle = getConfigBoolean("random", false);
		mimicDirection = getConfigBoolean("mimic-direction", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			spin(livingEntity, target.getTarget());
			playSpellEffects(livingEntity, target.getTarget());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		playSpellEffects(caster, target);
		spin(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playSpellEffects(EffectPosition.TARGET, target);
		spin(target);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		playSpellEffects(EffectPosition.TARGET, target);
		spin(caster, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private void spin(LivingEntity target) {
		Location loc = target.getLocation();
		if (randomAngle) {
			loc.setYaw(Util.getRandomInt(360));
			if (affectPitch) loc.setPitch(random.nextInt(181) - 90);
		} else {
			loc.setYaw(loc.getYaw() + rotationYaw);
			if (affectPitch) loc.setPitch(loc.getPitch() + rotationPitch);
		}
		target.teleport(loc);
	}

	private void spin(LivingEntity caster, LivingEntity target) {
		Location targetLoc = target.getLocation();
		Location casterLoc = caster.getLocation();

		if (faceTarget) caster.teleport(changeDirection(casterLoc, targetLoc));
		else if (faceCaster) target.teleport(changeDirection(targetLoc, casterLoc));
		else spin(target);
	}

	private void spin(LivingEntity entity, Location target) {
		entity.teleport(changeDirection(entity.getLocation(), target));
	}

	private Location changeDirection(Location caster, Location target) {
		Location loc = caster.clone();
		if (mimicDirection) {
			if (affectPitch) loc.setPitch(target.getPitch());
			loc.setYaw(target.getYaw());
		} else {
			loc.setDirection(getVectorDir(caster, target));
			if (!affectPitch) loc.setPitch(caster.getPitch());
		}
		return loc;
	}

	private Vector getVectorDir(Location caster, Location target) {
		return target.clone().subtract(caster.toVector()).toVector();
	}

}
