package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import com.nisovin.magicspells.util.wrapper.WrappedMaterial;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class PulserSpell extends TargetedSpell implements TargetedLocationSpell {

	private Map<Block, Pulser> pulsers;
	private Material material;
	private String materialName;

	private int yOffset;
	private int interval;
	private int totalPulses;
	private int capPerPlayer;

	private double maxDistanceSquared;

	private boolean unbreakable;
	private boolean onlyCountOnSuccess;

	private List<String> spellNames;
	private List<TargetedLocationSpell> spells;

	private String spellNameOnBreak;
	private TargetedLocationSpell spellOnBreak;

	private String strAtCap;

	private PulserTicker ticker;

	public PulserSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		materialName = getConfigString("block-type", "DIAMOND_BLOCK").toUpperCase();
		material = Material.getMaterial(materialName);
		if (material == null || !material.isBlock()) {
			MagicSpells.error("PulserSpell '" + internalName + "' has an invalid block-type defined");
			material = null;
		}

		yOffset = getConfigInt("y-offset", 0);
		interval = getConfigInt("interval", 30);
		totalPulses = getConfigInt("total-pulses", 5);
		capPerPlayer = getConfigInt("cap-per-player", 10);

		maxDistanceSquared = getConfigDouble("max-distance", 30);
		maxDistanceSquared *= maxDistanceSquared;

		unbreakable = getConfigBoolean("unbreakable", false);
		onlyCountOnSuccess = getConfigBoolean("only-count-on-success", false);

		spellNames = getConfigStringList("spells", null);
		spellNameOnBreak = getConfigString("spell-on-break", "");

		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");

		pulsers = new HashMap<>();
		ticker = new PulserTicker();
	}

	@Override
	public void initialize() {
		super.initialize();

		spells = new ArrayList<>();
		if (spellNames != null && !spellNames.isEmpty()) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (!(spell instanceof TargetedLocationSpell)) continue;
				spells.add((TargetedLocationSpell) spell);
			}
		}

		if (!spellNameOnBreak.isEmpty()) {
			Spell spell = MagicSpells.getSpellByInternalName(spellNameOnBreak);
			if (spell instanceof TargetedLocationSpell) spellOnBreak = (TargetedLocationSpell) spell;
			else MagicSpells.error("PulserSpell '" + internalName + "' has an invalid spell-on-break defined");
		}

		if (spells.isEmpty()) MagicSpells.error("PulserSpell '" + internalName + "' has no spells defined!");
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (capPerPlayer > 0) {
				int count = 0;
				for (Pulser pulser : pulsers.values()) {
					if (!pulser.caster.equals(livingEntity)) continue;
					
					count++;
					if (count >= capPerPlayer) {
						sendMessage(strAtCap, livingEntity, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
			List<Block> lastTwo = getLastTwoTargetedBlocks(livingEntity, power);
			Block target = null;

			if (lastTwo != null && lastTwo.size() == 2) target = lastTwo.get(0);
			if (target == null) return noTarget(livingEntity);
			if (yOffset > 0) target = target.getRelative(BlockFace.UP, yOffset);
			else if (yOffset < 0) target = target.getRelative(BlockFace.DOWN, yOffset);
			if (!BlockUtils.isAir(target.getType()) && target.getType() != Material.SNOW && target.getType() != WrappedMaterial.getWrapper().getTallGrass()) return noTarget(livingEntity);

			if (target != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, livingEntity, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) return noTarget(livingEntity);
				target = event.getTargetLocation().getBlock();
				power = event.getPower();
			}
			createPulser(livingEntity, target, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, Power power) {
		Block block = target.getBlock();
		if (yOffset > 0) block = block.getRelative(BlockFace.UP, yOffset);
		else if (yOffset < 0) block = block.getRelative(BlockFace.DOWN, yOffset);

		if (BlockUtils.isAir(block.getType()) || block.getType() == Material.SNOW || block.getType() == WrappedMaterial.getWrapper().getTallGrass()) {
			createPulser(caster, block, power);
			return true;
		}
		block = block.getRelative(BlockFace.UP);
		if (BlockUtils.isAir(block.getType()) || block.getType() == Material.SNOW || block.getType() == WrappedMaterial.getWrapper().getTallGrass()) {
			createPulser(caster, block, power);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, Power power) {
		return castAtLocation(null, target, power);
	}

	private void createPulser(LivingEntity caster, Block block, Power power) {
		if (material == null) return;
		block.setType(material);
		pulsers.put(block, new Pulser(caster, block, power));
		ticker.start();
		if (caster != null) playSpellEffects(caster, block.getLocation().add(0.5, 0.5, 0.5));
		else playSpellEffects(EffectPosition.TARGET, block.getLocation().add(0.5, 0.5, 0.5));
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Pulser pulser = pulsers.get(event.getBlock());
		if (pulser == null) return;
		event.setCancelled(true);
		if (unbreakable) return;
		pulser.stop();
		event.getBlock().setType(Material.AIR);
		pulsers.remove(event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (pulsers.isEmpty()) return;
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block b = iter.next();
			Pulser pulser = pulsers.get(b);
			if (pulser == null) continue;
			iter.remove();

			if (unbreakable) continue;
			pulser.stop();
			pulsers.remove(b);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPiston(BlockPistonExtendEvent event) {
		if (pulsers.isEmpty()) return;
		for (Block b : event.getBlocks()) {
			Pulser pulser = pulsers.get(b);
			if (pulser == null) continue;
			event.setCancelled(true);
			if (unbreakable) continue;
			pulser.stop();
			pulsers.remove(b);
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (pulsers.isEmpty()) return;
		Player player = event.getEntity();
		Iterator<Pulser> iter = pulsers.values().iterator();
		while (iter.hasNext()) {
			Pulser pulser = iter.next();
			if (pulser.caster == null) continue;
			if (!pulser.caster.equals(player)) continue;
			pulser.stop();
			iter.remove();
		}
	}

	@Override
	public void turnOff() {
		for (Pulser p : new ArrayList<>(pulsers.values())) {
			p.stop();
		}
		pulsers.clear();
		ticker.stop();
	}
	
	private class Pulser {

		private LivingEntity caster;
		private Block block;
		private Location location;
		private Power power;
		private int pulseCount;
		
		private Pulser(LivingEntity caster, Block block, Power power) {
			this.caster = caster;
			this.block = block;
			this.location = block.getLocation().add(0.5, 0.5, 0.5);
			this.power = power;
			this.pulseCount = 0;
		}

		private boolean pulse() {
			if (caster == null) {
				if (material.equals(block.getType()) && block.getChunk().isLoaded()) return activate();
				stop();
				return true;
			} else if (caster.isValid() && material.equals(block.getType()) && block.getChunk().isLoaded()) {
				if (maxDistanceSquared > 0 && (!LocationUtil.isSameWorld(location, caster) || location.distanceSquared(caster.getLocation()) > maxDistanceSquared)) {
					stop();
					return true;
				}
				return activate();
			}
			stop();
			return true;
		}
		
		private boolean activate() {
			boolean activated = false;
			for (TargetedLocationSpell spell : spells) {
				if (caster != null) activated = spell.castAtLocation(caster, location, power) || activated;
				else activated = spell.castAtLocation(location, power) || activated;
			}
			playSpellEffects(EffectPosition.DELAYED, location);
			if (totalPulses > 0 && (activated || !onlyCountOnSuccess)) {
				pulseCount += 1;
				if (pulseCount >= totalPulses) {
					stop();
					return true;
				}
			}
			return false;
		}

		private void stop() {
			if (!block.getChunk().isLoaded()) block.getChunk().load();
			block.setType(Material.AIR);
			playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, block.getLocation());
			if (spellOnBreak == null) return;
			if (caster == null) spellOnBreak.castAtLocation(location, power);
			else if (caster.isValid()) spellOnBreak.castAtLocation(caster, location, power);
		}

	}
	
	private class PulserTicker implements Runnable {

		private int taskId = -1;

		private void start() {
			if (taskId < 0) taskId = MagicSpells.scheduleRepeatingTask(this, 0, interval);
		}

		private void stop() {
			if (taskId > 0) {
				MagicSpells.cancelTask(taskId);
				taskId = -1;
			}
		}

		@Override
		public void run() {
			for (Map.Entry<Block, Pulser> entry : new HashMap<>(pulsers).entrySet()) {
				boolean remove = entry.getValue().pulse();
				if (remove) pulsers.remove(entry.getKey());
			}
			if (pulsers.isEmpty()) stop();
		}
		
	}

}
