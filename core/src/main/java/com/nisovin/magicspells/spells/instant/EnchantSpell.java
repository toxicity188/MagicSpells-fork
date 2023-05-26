package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.power.Power;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class EnchantSpell extends InstantSpell {
	
	private Map<Enchantment, Integer> enchantments;

	private List<String> enchantmentList;

	private boolean safeEnchants;
	
	public EnchantSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		enchantments = new HashMap<>();

		enchantmentList = getConfigStringList("enchantments", null);

		safeEnchants = getConfigBoolean("safe-enchants", true);

		if (enchantmentList != null && !enchantmentList.isEmpty()) {
			for (String string : enchantmentList) {
				Enchantment enchant = null;
				int level = 1;
				String[] str = string.split(" ");
				if (str[0] != null) enchant = Enchantment.getByName(str[0].toLowerCase());
				if (str.length > 1 && str[1] != null) level = Integer.valueOf(str[1]);
				if (enchant != null) enchantments.put(enchant, level);
			}
		} else MagicSpells.error("EnchantSpell '" + internalName + "' has invalid enchantments defined!");
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		ItemStack targetItem = livingEntity.getEquipment().getItemInMainHand();
		if (targetItem == null) return PostCastAction.ALREADY_HANDLED;
		enchant(targetItem);
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void enchant(ItemStack item) {
		for (Enchantment e : enchantments.keySet()) {
			enchant(item, e, enchantments.get(e));
		}
	}
	
	private void enchant(ItemStack item, Enchantment enchant, int level) {
		if (!enchant.canEnchantItem(item)) return;
		if (safeEnchants && level > enchant.getMaxLevel()) level = enchant.getMaxLevel();
		if (level <= 0) item.removeEnchantment(enchant);
		else {
			if (safeEnchants) item.addEnchantment(enchant, level);
			else item.addUnsafeEnchantment(enchant, level);
		}
	}

}
