package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import com.nisovin.magicspells.power.Power;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;

public class OffhandCooldownSpell extends InstantSpell implements SingleSpellSupplier {

	private List<Player> players = new ArrayList<>();

	private ItemStack item;

	private Spell spellToCheck;
	private String spellToCheckName;

	@Override
	public Spell getSubSpell() {
		return spellToCheck;
	}

	public OffhandCooldownSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		if (isConfigString("item")) item = Util.getItemStackFromString(getConfigString("item", "stone"));
		else if (isConfigSection("item")) item = Util.getItemStackFromConfig(getConfigSection("item"));

		spellToCheckName = getConfigString("spell", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellToCheck = MagicSpells.getSpellByInternalName(spellToCheckName);

		if (spellToCheck == null || item == null) return;
		
		MagicSpells.scheduleRepeatingTask(() -> {
			Iterator<Player> iter = players.iterator();
			while (iter.hasNext()) {
				Player pl = iter.next();
				if (!pl.isValid()) {
					iter.remove();
					continue;
				}
				float cd = spellToCheck.getCooldown(pl);
				int amt = 1;
				if (cd > 0) amt = (int) Math.ceil(cd);

				PlayerInventory inventory = pl.getInventory();
				ItemStack off = inventory.getItemInOffHand();
				off.setAmount(amt);
				if (off == null || !off.isSimilar(item)) inventory.setItemInOffHand(item.clone());
			}
		}, TimeUtil.TICKS_PER_SECOND, TimeUtil.TICKS_PER_SECOND);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			players.add((Player) livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
