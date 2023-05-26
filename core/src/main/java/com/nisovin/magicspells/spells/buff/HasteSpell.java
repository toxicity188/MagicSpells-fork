package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class HasteSpell extends BuffSpell {

	private Map<UUID, Integer> hasted;

	private int strength;
	private int boostDuration;

	public HasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strength = getConfigInt("effect-strength", 3);
		boostDuration = getConfigInt("boost-duration", 300);

		hasted = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, Power power, String[] args) {
		hasted.put(entity.getUniqueId(), Math.round(strength * power.intValue()));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return hasted.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		if (hasted.remove(entity.getUniqueId()) == null) return;
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1, 0), true);
		entity.removePotionEffect(PotionEffectType.SPEED);
	}

	@Override
	protected void turnOff() {
		for (UUID id : hasted.keySet()) {
			Entity e = Bukkit.getEntity(id);
			if (!(e instanceof LivingEntity)) continue;
			LivingEntity livingEntity = (LivingEntity) e;
			livingEntity.removePotionEffect(PotionEffectType.SPEED);
		}

		hasted.clear();
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		Player player = event.getPlayer();
		Integer amplifier = hasted.get(player.getUniqueId());
		if (amplifier == null) return;

		if (isExpired(player)) {
			turnOff(player);
			return;
		}

		if (event.isSprinting()) {
			event.setCancelled(true);
			addUseAndChargeCost(player);
			playSpellEffects(EffectPosition.CASTER, player);
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, boostDuration, amplifier), true);
		} else {
			player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1, 0), true);
			player.removePotionEffect(PotionEffectType.SPEED);
			playSpellEffects(EffectPosition.DISABLED, player);
		}
	}

}
