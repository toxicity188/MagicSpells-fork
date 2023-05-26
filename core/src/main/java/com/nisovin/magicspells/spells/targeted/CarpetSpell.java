package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spells.SingleSpellSupplier;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class CarpetSpell extends TargetedSpell implements TargetedLocationSpell, SingleSpellSupplier {

	private Map<Block, Player> blocks;

	private Material material;
	private String materialName;

	private int radius;
	private int duration;
	private int touchCheckInterval;

	private boolean circle;
	private boolean removeOnTouch;

	private String spellOnTouchName;
	private Subspell spellOnTouch;

	private TouchChecker checker;

	@Override
	public Spell getSubSpell() {
		return spellOnTouch != null ? spellOnTouch.getSpell() : null;
	}

	public CarpetSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		materialName = getConfigString("block", "white_carpet").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicSpells.error("CarpetSpell '" + internalName + "' has an invalid block defined!");
			material = null;
		}

		radius = getConfigInt("radius", 1);
		duration = getConfigInt("duration", 0);
		touchCheckInterval = getConfigInt("touch-check-interval", 3);

		circle = getConfigBoolean("circle", false);
		removeOnTouch = getConfigBoolean("remove-on-touch", true);

		spellOnTouchName = getConfigString("spell-on-touch", "");

		blocks = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellOnTouch = new Subspell(spellOnTouchName);
		if (!spellOnTouch.process() || !spellOnTouch.isTargetedEntitySpell()) {
			if (!spellOnTouchName.isEmpty()) MagicSpells.error("CarpetSpell '" + internalName + "' has an invalid spell-on-touch defined!");
			spellOnTouch = null;
		}

		if (spellOnTouch != null) checker = new TouchChecker();
	}
	
	@Override
	public void turnOff() {
		super.turnOff();

		blocks.clear();
		if (checker != null) checker.stop();
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			Location loc = null;
			if (targetSelf) loc = player.getLocation();
			else {
				Block b = getTargetedBlock(player, power);
				if (b != null && b.getType() != Material.AIR) loc = b.getLocation();
			}

			if (loc == null) return noTarget(player);

			layCarpet(player, loc, power);
		}
		return PostCastAction.ALREADY_HANDLED;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		if (!(caster instanceof Player)) return false;
		if (targetSelf) layCarpet((Player) caster, caster.getLocation(), power);
		else layCarpet((Player) caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		layCarpet(null, target, power);
		return true;
	}
	
	private void layCarpet(Player player, Location loc, Power power) {
		if (!loc.getBlock().getType().isOccluding()) {
			int c = 0;
			while (!loc.getBlock().getRelative(0, -1, 0).getType().isOccluding() && c <= 2) {
				loc.subtract(0, 1, 0);
				c++;
			}
		} else {
			int c = 0;
			while (loc.getBlock().getType().isOccluding() && c <= 2) {
				loc.add(0, 1, 0);
				c++;
			}
		}

		Block b;
		int y = loc.getBlockY();
		int rad = Math.round(radius * power.intValue());

		final List<Block> blockList = new ArrayList<>();
		for (int x = loc.getBlockX() - rad; x <= loc.getBlockX() + rad; x++) {
			for (int z = loc.getBlockZ() - rad; z <= loc.getBlockZ() + rad; z++) {
				b = loc.getWorld().getBlockAt(x, y, z);
				if (circle && loc.getBlock().getLocation().distanceSquared(b.getLocation()) > radius * radius) continue;

				if (b.getType().isOccluding()) b = b.getRelative(0, 1, 0);
				else if (!b.getRelative(0, -1, 0).getType().isOccluding()) b = b.getRelative(0, -1, 0);

				if (!BlockUtils.isAir(b.getType()) && !b.getRelative(0, -1, 0).getType().isSolid()) continue;

				b.setType(material, false);
				blockList.add(b);
				blocks.put(b, player);
				playSpellEffects(EffectPosition.TARGET, b.getLocation().add(0.5, 0, 0.5));
			}
		}

		if (duration > 0 && !blockList.isEmpty()) {
			MagicSpells.scheduleDelayedTask(() -> {
				for (Block b1 : blockList) {
					if (!material.equals(b1.getType())) continue;
					b1.setType(Material.AIR);
					if (blocks != null) blocks.remove(b1);
				}
			}, duration);
		}
		if (player != null) playSpellEffects(EffectPosition.CASTER, player);
	}
	
	private class TouchChecker implements Runnable {
		
		private int taskId;
		
		private TouchChecker() {
			taskId = MagicSpells.scheduleRepeatingTask(this, touchCheckInterval, touchCheckInterval);
		}
		
		@Override
		public void run() {
			if (blocks.isEmpty()) return;
			for (Player player : Bukkit.getOnlinePlayers()) {

				Block b = player.getLocation().getBlock();
				Player caster = blocks.get(b);

				if (caster == null) continue;
				if (player.equals(caster)) continue;
				if (!material.equals(b.getType())) continue;
				
				SpellTargetEvent event = new SpellTargetEvent(spellOnTouch.getSpell(), caster, player, new Power(1));
				EventUtil.call(event);
				if (event.isCancelled()) continue;

				if (spellOnTouch != null) spellOnTouch.castAtEntity(caster, player, event.getPower());
				if (!removeOnTouch) continue;

				b.setType(Material.AIR);
				blocks.remove(b);
			}
		}
		
		private void stop() {
			MagicSpells.cancelTask(taskId);
		}
		
	}

}
