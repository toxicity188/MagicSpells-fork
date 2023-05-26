package com.nisovin.magicspells.spells.buff;

import com.nisovin.magicspells.power.Power;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.events.ManaChangeEvent;

public class ManaRegenSpell extends BuffSpell { 

	private Set<UUID> regeners;

	private int regenModAmt;

	public ManaRegenSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		regenModAmt = getConfigInt("regen-mod-amt", 3);

		regeners = new HashSet<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, Power power, String[] args) {
		regeners.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return regeners.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		regeners.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		regeners.clear();
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onManaRegenTick(ManaChangeEvent event) {
		Player pl = event.getPlayer();
		if (isExpired(pl)) {
			turnOff(pl);
			return;
		}

		if (!isActive(pl)) return;
		if (!event.getReason().equals(ManaChangeReason.REGEN)) return;
		
		int newAmt = event.getNewAmount() + regenModAmt;
		if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
		else if (newAmt < 0) newAmt = 0;

		addUseAndChargeCost(pl);
		event.setNewAmount(newAmt);
	}

}
