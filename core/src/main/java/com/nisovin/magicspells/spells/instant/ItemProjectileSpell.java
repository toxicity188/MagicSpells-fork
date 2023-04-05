package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ItemProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	private List<Item> itemList;

	private ItemStack item;

	private int spellDelay;
	private int pickupDelay;
	private int removeDelay;
	private int tickInterval;
	private int spellInterval;
	private int itemNameDelay;
	private int specialEffectInterval;

	private float speed;
	private float yOffset;
	private float hitRadius;
	private float vertSpeed;
	private float vertHitRadius;
	private float rotationOffset;

	private boolean vertSpeedUsed;
	private boolean stopOnHitGround;
	private boolean projectileHasGravity;

	private Vector relativeOffset;

	private String itemName;
	private String spellOnTickName;
	private String spellOnDelayName;
	private String spellOnHitEntityName;
	private String spellOnHitGroundName;

	private Subspell spellOnTick;
	private Subspell spellOnDelay;
	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;

	public ItemProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		itemList = new ArrayList<>();

		item = Util.getItemStackFromString(getConfigString("item", "iron_sword"));

		spellDelay = getConfigInt("spell-delay", 40);
		pickupDelay = getConfigInt("pickup-delay", 100);
		removeDelay = getConfigInt("remove-delay", 100);
		tickInterval = getConfigInt("tick-interval", 1);
		spellInterval = getConfigInt("spell-interval", 2);
		itemNameDelay = getConfigInt("item-name-delay", 10);
		specialEffectInterval = getConfigInt("special-effect-interval", 2);

		speed = getConfigFloat("speed", 1F);
		yOffset = getConfigFloat("y-offset", 0F);
		hitRadius = getConfigFloat("hit-radius", 1F);
		vertSpeed = getConfigFloat("vert-speed", 0F);
		vertHitRadius = getConfigFloat("vertical-hit-radius", 1.5F);
		rotationOffset = getConfigFloat("rotation-offset", 0F);

		if (vertSpeed != 0) vertSpeedUsed = true;
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		projectileHasGravity = getConfigBoolean("gravity", true);

		relativeOffset = getConfigVector("relative-offset", "0,0,0");
		if (yOffset != 0) relativeOffset.setY(yOffset);

		itemName = ChatColor.translateAlternateColorCodes('&', getConfigString("item-name", ""));
		spellOnTickName = getConfigString("spell-on-tick", "");
		spellOnDelayName = getConfigString("spell-on-delay", "");
		spellOnHitEntityName = getConfigString("spell-on-hit-entity", "");
		spellOnHitGroundName = getConfigString("spell-on-hit-ground", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellOnTick = new Subspell(spellOnTickName);
		if (!spellOnTick.process()) {
			if (!spellOnTickName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-tick defined!");
			spellOnTick = null;
		}

		spellOnDelay = new Subspell(spellOnDelayName);
		if (!spellOnDelay.process()) {
			if (!spellOnDelayName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-delay defined!");
			spellOnDelay = null;
		}

		spellOnHitEntity = new Subspell(spellOnHitEntityName);
		if (!spellOnHitEntity.process()) {
			if (!spellOnHitEntityName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
			spellOnHitEntity = null;
		}

		spellOnHitGround = new Subspell(spellOnHitGroundName);
		if (!spellOnHitGround.process()) {
			if (!spellOnHitGroundName.isEmpty()) MagicSpells.error("ItemProjectileSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			spellOnHitGround = null;
		}
	}

	@Override
	public void turnOff() {
		for (Item item : itemList) {
			item.remove();
		}

		itemList.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			new ItemProjectile(livingEntity, livingEntity.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location target, float power) {
		new ItemProjectile(livingEntity, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private class ItemProjectile implements Runnable {

		private LivingEntity caster;
		private Item entity;
		private Vector velocity;
		private Location startLocation;
		private Location currentLocation;
		private float power;

		private boolean landed = false;
		private boolean groundSpellCasted = false;

		private int taskId;
		private int count = 0;

		private ItemProjectile(LivingEntity caster, Location from, float power) {
			this.caster = caster;
			this.power = power;

			startLocation = from.clone();

			//relativeOffset
			Vector startDirection = from.getDirection().normalize();
			Vector horizOffset = new Vector(-startDirection.getZ(), 0.0, startDirection.getX()).normalize();
			startLocation.add(horizOffset.multiply(relativeOffset.getZ())).getBlock().getLocation();
			startLocation.add(startLocation.getDirection().multiply(relativeOffset.getX()));
			startLocation.setY(startLocation.getY() + relativeOffset.getY());

			currentLocation = startLocation.clone();

			if (vertSpeedUsed) velocity = from.getDirection().setY(0).multiply(speed).setY(vertSpeed);
			else velocity = from.getDirection().multiply(speed);
			Util.rotateVector(velocity, rotationOffset);
			entity = from.getWorld().dropItem(startLocation, item.clone());
			entity.setGravity(projectileHasGravity);
			entity.setPickupDelay(pickupDelay);
			entity.setVelocity(velocity);
			itemList.add(entity);

			playSpellEffects(EffectPosition.PROJECTILE, entity);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, from, entity.getLocation(), caster, entity);
			
			taskId = MagicSpells.scheduleRepeatingTask(this, tickInterval, tickInterval);

			MagicSpells.scheduleDelayedTask(() -> {
				entity.setCustomName(itemName);
				entity.setCustomNameVisible(true);
			}, itemNameDelay);

			MagicSpells.scheduleDelayedTask(this::stop, removeDelay);
		}
		
		@Override
		public void run() {
			if (entity == null || !entity.isValid() || entity.isDead()) {
				stop();
				return;
			}

			count++;

			currentLocation = entity.getLocation();
			currentLocation.setDirection(entity.getVelocity());
			if (specialEffectInterval > 0 && count % specialEffectInterval == 0) playSpellEffects(EffectPosition.SPECIAL, currentLocation);

			if (count % spellInterval == 0 && spellOnTick != null && spellOnTick.isTargetedLocationSpell()) {
				spellOnTick.castAtLocation(caster, currentLocation.clone(), power);
			}

			for (Entity e : entity.getNearbyEntities(hitRadius, vertHitRadius, hitRadius)) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;

				SpellTargetEvent event = new SpellTargetEvent(ItemProjectileSpell.this, caster, (LivingEntity) e, power);
				EventUtil.call(event);
				if (!event.isCancelled()) {
					if (spellOnHitEntity != null) spellOnHitEntity.castAtEntity(caster, (LivingEntity) e, event.getPower());
					stop();
					return;
				}
			}

			if (entity.isOnGround()) {
				if (spellOnHitGround != null && !groundSpellCasted) {
					spellOnHitGround.castAtLocation(caster, entity.getLocation(), power);
					groundSpellCasted = true;
				}
				if (stopOnHitGround) {
					stop();
					return;
				}
				if (!landed) MagicSpells.scheduleDelayedTask(() -> {
					if (spellOnDelay != null) spellOnDelay.castAtLocation(caster, entity.getLocation(), power);
					stop();
				}, spellDelay);
				landed = true;
			}
		}

		private void stop() {
			itemList.remove(entity);
			entity.remove();
			MagicSpells.cancelTask(taskId);
		}
		
	}

}
