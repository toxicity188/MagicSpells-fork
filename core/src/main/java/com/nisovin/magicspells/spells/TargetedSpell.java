package com.nisovin.magicspells.spells;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.power.Power;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TxtUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.ValidTargetChecker;

public abstract class TargetedSpell extends InstantSpell {

	static private Pattern chatVarCasterMatchPattern = Pattern.compile("%castervar:[A-Za-z0-9_]+(:[0-9]+)?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	static private Pattern chatVarTargetMatchPattern = Pattern.compile("%targetvar:[A-Za-z0-9_]+(:[0-9]+)?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	protected boolean targetSelf;
	protected boolean alwaysActivate;
	protected boolean playFizzleSound;
	
	protected String spellNameOnFail;
	protected Subspell spellOnFail;

	protected String strNoTarget;
	protected String strCastTarget;

	public TargetedSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		targetSelf = getConfigBoolean("target-self", false);
		alwaysActivate = getConfigBoolean("always-activate", false);
		playFizzleSound = getConfigBoolean("play-fizzle-sound", false);

		spellNameOnFail = getConfigString("spell-on-fail", "");

		strNoTarget = getConfigString("str-no-target", "");
		strCastTarget = getConfigString("str-cast-target", "");

	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (spellNameOnFail.isEmpty()) return;

		spellOnFail = new Subspell(spellNameOnFail);
		if (!spellOnFail.process()) {
			spellOnFail = null;
			MagicSpells.error("Spell '" + internalName + "' has an invalid spell-on-fail defined!");
		}
	}
	
	protected void sendMessages(LivingEntity caster, LivingEntity target) {
		if (!(caster instanceof Player)) return;
		String targetName = getTargetName(target);
		Player playerTarget = null;
		if (target instanceof Player) playerTarget = (Player) target;
		sendMessage(prepareMessage(strCastSelf, (Player) caster, targetName, playerTarget), caster, MagicSpells.NULL_ARGS);
		if (playerTarget != null) sendMessage(prepareMessage(strCastTarget, (Player) caster, targetName, playerTarget), playerTarget, MagicSpells.NULL_ARGS);
		sendMessageNear(caster, playerTarget, prepareMessage(strCastOthers, (Player) caster, targetName, playerTarget), broadcastRange, MagicSpells.NULL_ARGS);
	}
	
	private String prepareMessage(String message, Player caster, String targetName, Player playerTarget) {
		if (message == null || message.isEmpty()) return message;
		message = message.replace("%a", caster.getName());
		message = message.replace("%t", targetName);
		if (playerTarget != null && MagicSpells.getVariableManager() != null && message.contains("%targetvar")) {
			Matcher matcher = chatVarTargetMatchPattern.matcher(message);
			while (matcher.find()) {
				String varText = matcher.group();
				String[] varData = varText.substring(5, varText.length() - 1).split(":");
				String val = MagicSpells.getVariableManager().getStringValue(varData[0], playerTarget);
				String sval = varData.length == 1 ? TxtUtil.getStringNumber(val, -1) : TxtUtil.getStringNumber(val, Integer.parseInt(varData[1]));
				message = message.replace(varText, sval);
			}
		}
		if (MagicSpells.getVariableManager() != null && message.contains("%castervar")) {
			Matcher matcher = chatVarCasterMatchPattern.matcher(message);
			while (matcher.find()) {
				String varText = matcher.group();
				String[] varData = varText.substring(5, varText.length() - 1).split(":");
				String val = MagicSpells.getVariableManager().getStringValue(varData[0], caster);
				String sval = varData.length == 1 ? TxtUtil.getStringNumber(val, -1) : TxtUtil.getStringNumber(val, Integer.parseInt(varData[1]));
				message = message.replace(varText, sval);
			}
		}

		return message;
	}
	
	protected String getTargetName(LivingEntity target) {
		if (target instanceof Player) return target.getName();
		String name = MagicSpells.getEntityNames().get(target.getType());
		if (name != null) return name;
		return "unknown";
	}
	
	/**
	 * Checks whether two locations are within a certain distance from each other.
	 * @param loc1 The first location
	 * @param loc2 The second location
	 * @param range The maximum distance
	 * @return true if the distance is less than the range, false otherwise
	 */
	protected boolean inRange(Location loc1, Location loc2, int range) {
		return loc1.distanceSquared(loc2) < range * range;
	}
	
	/**
	 * Plays the fizzle sound if it is enabled for this spell.
	 */
	protected void fizzle(LivingEntity livingEntity) {
		if (playFizzleSound && livingEntity instanceof Player) ((Player) livingEntity).playEffect(livingEntity.getLocation(), Effect.EXTINGUISH, null);
	}
	
	@Override
	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity livingEntity, Power power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		if (targetSelf || validTargetList.canTargetSelf()) return new TargetInfo<>(livingEntity, power);
		return super.getTargetedEntity(livingEntity, power, forceTargetPlayers, checker);
	}
	
	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 * @param livingEntity the casting living entity
	 * @return the appropriate PostcastAction value
	 */
	protected PostCastAction noTarget(LivingEntity livingEntity) {
		return noTarget(livingEntity, strNoTarget);
	}
	
	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 * @param livingEntity the casting living entity
	 * @param message the message to send
	 * @return
	 */
	protected PostCastAction noTarget(LivingEntity livingEntity, String message) {
		fizzle(livingEntity);
		sendMessage(message, livingEntity, MagicSpells.NULL_ARGS);
		if (spellOnFail != null) spellOnFail.cast(livingEntity, new Power(1));
		return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
	}
	
}
