package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.EnumSet;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.events.MagicSpellsBlockBreakEvent;

public class ZapSpell extends TargetedSpell implements TargetedLocationSpell {

	private Set<Material> allowedBlockTypes;
	private Set<Material> disallowedBlockTypes;

	private String strCantZap;

	private boolean dropBlock;
	private boolean dropNormal;
	private boolean checkPlugins;
	private boolean playBreakEffect;
	
	public ZapSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> allowed = getConfigStringList("allowed-block-types", null);
		if (allowed != null) {
			allowedBlockTypes = EnumSet.noneOf(Material.class);
			for (String s : allowed) {
				Material m = Material.getMaterial(s.toUpperCase());
				if (m == null) continue;

				allowedBlockTypes.add(m);
			}
		}
		
		List<String> disallowed = getConfigStringList("disallowed-block-types", Arrays.asList("bedrock", "lava", "water"));
		if (disallowed != null) {
			disallowedBlockTypes = EnumSet.noneOf(Material.class);
			for (String s : disallowed) {
				Material m = Material.getMaterial(s.toUpperCase());
				if (m == null) continue;
				
				disallowedBlockTypes.add(m);
			}
		}

		strCantZap = getConfigString("str-cant-zap", "");
		
		dropBlock = getConfigBoolean("drop-block", false);
		dropNormal = getConfigBoolean("drop-normal", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
		playBreakEffect = getConfigBoolean("play-break-effect", true);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player) {
			Block target;
			try {
				target = getTargetedBlock(caster, power);
			} catch (IllegalStateException e) {
				target = null;
			}
			if (target != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) target = null;
				else target = event.getTargetLocation().getBlock();
			}
			if (target == null) return noTarget(caster, strCantZap);

			if (!canZap(target)) return noTarget(caster, strCantZap);
			boolean ok = zap(target, (Player) caster);
			if (!ok) return noTarget(caster, strCantZap);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		if (!(caster instanceof Player)) return false;
		Block block = target.getBlock();
		if (canZap(block)) {
			zap(block, (Player) caster);
			return true;
		}

		Vector v = target.getDirection();
		block = target.clone().add(v).getBlock();

		if (canZap(block)) {
			zap(block, (Player) caster);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		Block block = target.getBlock();
		if (canZap(block)) {
			zap(block, null);
			return true;
		}
		return false;
	}

	private boolean zap(Block target, Player player) {
		boolean playerNull = player == null;

		if (checkPlugins && !playerNull) {
			MagicSpellsBlockBreakEvent event = new MagicSpellsBlockBreakEvent(target, player);
			MagicSpells.plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) return false;
		}

		if (dropBlock) {
			if (dropNormal) target.breakNaturally();
			else target.getWorld().dropItemNaturally(target.getLocation(), target.getState().getData().toItemStack(1));
		}

		if (playBreakEffect) target.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getType());
		if (!playerNull) playSpellEffects(EffectPosition.CASTER, player);
		playSpellEffects(EffectPosition.TARGET, target.getLocation());
		if (!playerNull) playSpellEffectsTrail(player.getLocation(), target.getLocation());

		target.setType(Material.AIR);
		return true;
	}
	
	private boolean canZap(Block target) {
		Material type = target.getType();
		if (disallowedBlockTypes.contains(type)) return false;
		if (allowedBlockTypes.isEmpty()) return true;
		return allowedBlockTypes.contains(type);
	}
	
}
