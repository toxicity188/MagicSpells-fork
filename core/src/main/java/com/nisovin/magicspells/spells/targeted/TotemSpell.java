package com.nisovin.magicspells.spells.targeted;

import java.util.*;
import java.util.stream.Collectors;

import com.nisovin.magicspells.spells.SpellSupplier;
import com.nisovin.magicspells.util.wrapper.WrappedMaterial;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class TotemSpell extends TargetedSpell implements TargetedLocationSpell, SpellSupplier {

	private Set<Totem> totems;
	private PulserTicker ticker;

	private int yOffset;
	private int interval;
	private int totalPulses;
	private int capPerPlayer;

	private double maxDistanceSquared;

	private boolean gravity;
	private boolean visibility;
	private boolean targetable;
	private boolean onlyCountOnSuccess;

	private String strAtCap;
	private String totemName;

	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	private ItemStack hand;
	private ItemStack mainHand;

	private List<String> spellNames;


	@Override
	public List<Spell> getSubSpells() {
		return spells != null ? spells.stream().map(i -> (Spell) i).collect(Collectors.toList()) : Collections.emptyList();
	}

	private List<TargetedLocationSpell> spells;

	private String spellNameOnBreak;
	private TargetedLocationSpell spellOnBreak;

	public TotemSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		helmet = Util.getItemStackFromString(getConfigString("helmet", "AIR"));
		chestplate = Util.getItemStackFromString(getConfigString("chestplate", "AIR"));
		leggings = Util.getItemStackFromString(getConfigString("leggings", "AIR"));
		boots = Util.getItemStackFromString(getConfigString("boots", "AIR"));
		hand = Util.getItemStackFromString(getConfigString("hand", "AIR"));
		mainHand = Util.getItemStackFromString(getConfigString("main-hand", "AIR"));

		if (helmet != null && !BlockUtils.isAir(helmet.getType())) helmet.setAmount(1);
		if (chestplate != null && !BlockUtils.isAir(chestplate.getType())) chestplate.setAmount(1);
		if (leggings != null && !BlockUtils.isAir(leggings.getType())) leggings.setAmount(1);
		if (boots != null && !BlockUtils.isAir(boots.getType())) boots.setAmount(1);
		if (hand != null && !BlockUtils.isAir(hand.getType())) hand.setAmount(1);
		if (mainHand != null && !BlockUtils.isAir(mainHand.getType())) mainHand.setAmount(1);

		yOffset = getConfigInt("y-offset", 0);
		interval = getConfigInt("interval", 30);
		totalPulses = getConfigInt("total-pulses", 5);
		capPerPlayer = getConfigInt("cap-per-player", 10);

		maxDistanceSquared = getConfigDouble("max-distance", 30);
		maxDistanceSquared *= maxDistanceSquared;

		gravity = getConfigBoolean("gravity", false);
		visibility = getConfigBoolean("visible", true);
		targetable = getConfigBoolean("targetable", true);
		onlyCountOnSuccess = getConfigBoolean("only-count-on-success", false);

		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");
		totemName = getConfigString("totem-name", "");

		spellNames = getConfigStringList("spells", null);
		spellNameOnBreak = getConfigString("spell-on-break", "");

		totems = new HashSet<>();
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
			else MagicSpells.error("TotemSpell '" + internalName + "' has an invalid spell-on-break defined");
		}

		if (spells.isEmpty()) MagicSpells.error("TotemSpell '" + internalName + "' has no spells defined!");
	}

	@Override
	public void turnOff() {
		for (Totem t : totems) {
			t.stop();
		}

		totems.clear();
		ticker.stop();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (capPerPlayer > 0) {
				int count = 0;
				for (Totem pulser : totems) {
					if (!pulser.caster.equals(caster)) continue;

					count++;
					if (count >= capPerPlayer) {
						sendMessage(strAtCap, caster, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}

			List<Block> lastTwo = getLastTwoTargetedBlocks(caster, power);
			Block target = null;

			if (lastTwo != null && lastTwo.size() == 2) target = lastTwo.get(0);
			if (target == null) return noTarget(caster);
			if (yOffset > 0) target = target.getRelative(BlockFace.UP, yOffset);
			else if (yOffset < 0) target = target.getRelative(BlockFace.DOWN, yOffset);
			if (!BlockUtils.isAir(target.getType()) && target.getType() != Material.SNOW && target.getType() != WrappedMaterial.getWrapper().getTallGrass()) return noTarget(caster);

			if (target != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) return noTarget(caster);
				target = event.getTargetLocation().getBlock();
				power = event.getPower();
			}
			createTotem(caster, target.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		Block block = target.getBlock();
		if (yOffset > 0) block = block.getRelative(BlockFace.UP, yOffset);
		else if (yOffset < 0) block = block.getRelative(BlockFace.DOWN, yOffset);

		if (BlockUtils.isAir(block.getType()) || block.getType() == Material.SNOW || block.getType() == WrappedMaterial.getWrapper().getTallGrass()) {
			createTotem(caster, block.getLocation(), power);
			return true;
		}
		block = block.getRelative(BlockFace.UP);
		if (BlockUtils.isAir(block.getType()) || block.getType() == Material.SNOW || block.getType() == WrappedMaterial.getWrapper().getTallGrass()) {
			createTotem(caster, block.getLocation(), power);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}

	private void createTotem(LivingEntity caster, Location loc, float power) {
		totems.add(new Totem(caster, loc, power));
		ticker.start();
		if (caster != null) playSpellEffects(caster, loc);
		else playSpellEffects(EffectPosition.TARGET, loc);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (totems.isEmpty()) return;
		Player player = event.getEntity();
		Iterator<Totem> iter = totems.iterator();
		while (iter.hasNext()) {
			Totem pulser = iter.next();
			if (pulser.caster == null) continue;
			if (!pulser.caster.equals(player)) continue;
			pulser.stop();
			iter.remove();
		}
	}

	@EventHandler
	public void onSpellTarget(SpellTargetEvent e) {
		LivingEntity target = e.getTarget();
		if (totems.isEmpty()) return;
		for (Totem t : totems) {
			if (target.equals(t.armorStand) && !targetable) e.setCancelled(true);
			else if (e.getCaster().equals(t.caster) && target.equals(t.armorStand)) e.setCancelled(true);
		}
	}

	@EventHandler
	public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (totems.isEmpty()) return;
		for (Totem t : totems) {
			if (t.armorStand.equals(e.getRightClicked())) e.setCancelled(true);
		}
	}

	private class Totem {

		private LivingEntity caster;
		private LivingEntity armorStand;
		private Location totemLocation;
		private EntityEquipment totemEquipment;

		private float power;
		private int pulseCount;

		private Totem(LivingEntity caster, Location loc, float power) {
			this.caster = caster;
			this.power = power;

			pulseCount = 0;
			loc.setYaw(caster.getLocation().getYaw());
			armorStand = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
			if (!totemName.isEmpty()) {
				armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&', totemName));
				armorStand.setCustomNameVisible(true);
			}
			totemEquipment = armorStand.getEquipment();
			armorStand.setGravity(gravity);
			totemEquipment.setItemInMainHand(mainHand);
			totemEquipment.setItemInOffHand(hand);
			totemEquipment.setHelmet(helmet);
			totemEquipment.setChestplate(chestplate);
			totemEquipment.setLeggings(leggings);
			totemEquipment.setBoots(boots);
			((ArmorStand) armorStand).setVisible(visibility);
			armorStand.setInvulnerable(true);
			totemLocation = armorStand.getLocation();
		}

		private boolean pulse() {
			totemLocation = armorStand.getLocation();
			if (caster == null) {
				if (!armorStand.isDead()) return activate();
				stop();
				return true;
			} else if (caster.isValid() && !armorStand.isDead() && totemLocation.getChunk().isLoaded()) {
				if (maxDistanceSquared > 0 && (!LocationUtil.isSameWorld(totemLocation, caster) || totemLocation.distanceSquared(caster.getLocation()) > maxDistanceSquared)) {
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
				if (caster != null) activated = spell.castAtLocation(caster, totemLocation, power) || activated;
				else activated = spell.castAtLocation(totemLocation, power) || activated;
			}

			playSpellEffects(EffectPosition.SPECIAL, totemLocation);
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
			if (!totemLocation.getChunk().isLoaded()) totemLocation.getChunk().load();
			armorStand.remove();
			playSpellEffects(EffectPosition.DISABLED, totemLocation);
			if (spellOnBreak != null) {
				if (caster == null) spellOnBreak.castAtLocation(totemLocation, power);
				else if (caster.isValid()) spellOnBreak.castAtLocation(caster, totemLocation, power);
			}
		}

	}

	private class PulserTicker implements Runnable {

		private int taskId = -1;

		private void start() {
			if (taskId < 0) taskId = MagicSpells.scheduleRepeatingTask(this, 0, interval);
		}

		@Override
		public void run() {
			for (Totem p : new HashSet<>(totems)) {
				boolean remove = p.pulse();
				if (remove) totems.remove(p);
			}
			if (totems.isEmpty()) stop();
		}

		private void stop() {
			if (taskId > 0) {
				MagicSpells.cancelTask(taskId);
				taskId = -1;
			}
		}

	}

}
