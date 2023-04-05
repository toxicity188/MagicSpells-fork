package com.nisovin.magicspells.spells.targeted;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class ExplodeSpell extends TargetedSpell implements TargetedLocationSpell {
	
	private int explosionSize;
	private int backfireChance;

	private float damageMultiplier;

	private boolean addFire;
	private boolean simulateTnt;
	private boolean ignoreCanceled;
	private boolean preventBlockDamage;
	private boolean preventPlayerDamage;
	private boolean preventAnimalDamage;

	private long currentTick = 0;
	private float currentPower = 0;
	
	public ExplodeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		explosionSize = getConfigInt("explosion-size", 4);
		backfireChance = getConfigInt("backfire-chance", 0);

		damageMultiplier = getConfigFloat("damage-multiplier", 0);

		addFire = getConfigBoolean("add-fire", false);
		simulateTnt = getConfigBoolean("simulate-tnt", true);
		ignoreCanceled = getConfigBoolean("ignore-cancelled", false);
		preventBlockDamage = getConfigBoolean("prevent-block-damage", false);
		preventPlayerDamage = getConfigBoolean("prevent-player-damage", false);
		preventAnimalDamage = getConfigBoolean("prevent-animal-damage", false);
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target;
			try {
				target = getTargetedBlock(livingEntity, power);
			} catch (IllegalStateException e) {
				DebugHandler.debugIllegalState(e);
				target = null;
			}

			if (target != null && !BlockUtils.isAir(target.getType())) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, livingEntity, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) target = null;
				else {
					target = event.getTargetLocation().getBlock();
					power = event.getPower();
				}
			}

			if (target == null || BlockUtils.isAir(target.getType())) return noTarget(livingEntity);
			boolean exploded = explode(livingEntity, target.getLocation(), power);
			if (!exploded && !ignoreCanceled) return noTarget(livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean explode(LivingEntity livingEntity, Location target, float power) {
		if (simulateTnt) {
			boolean cancelled = MagicSpells.getVolatileCodeHandler().simulateTnt(target, livingEntity, explosionSize * power, addFire);
			if (cancelled) return false;
		}

		if (backfireChance > 0) {
			Random rand = new Random();
			if (rand.nextInt(10000) < backfireChance) target = livingEntity.getLocation();
		}

		currentTick = Bukkit.getWorlds().get(0).getFullTime();
		currentPower = power;

		boolean ret = false;
		if (livingEntity instanceof Player) {
			ret = MagicSpells.getVolatileCodeHandler().createExplosionByPlayer((Player) livingEntity, target, explosionSize * power, addFire, !preventBlockDamage);
		}
		if (ret) playSpellEffects(livingEntity, target);
		return ret;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return explode(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@EventHandler(priority=EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!(damageMultiplier > 0 || preventPlayerDamage)) return;
		if (!(event.getCause() == DamageCause.BLOCK_EXPLOSION || event.getCause() == DamageCause.ENTITY_EXPLOSION)) return;
		if (currentTick != Bukkit.getWorlds().get(0).getFullTime()) return;

		if (preventPlayerDamage && event.getEntity() instanceof Player) event.setCancelled(true);
		else if (preventAnimalDamage && event.getEntity() instanceof Animals) event.setCancelled(true);
		else if (damageMultiplier > 0) event.setDamage(Math.round(event.getDamage() * damageMultiplier * currentPower));
	}
	
	@EventHandler
	public void onExplode(EntityExplodeEvent event) {
		if (event.isCancelled() || !preventBlockDamage) return;
		if (currentTick == Bukkit.getWorlds().get(0).getFullTime()) {
			event.blockList().clear();
			event.setYield(0);
		}
	}
	
}
