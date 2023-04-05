package com.nisovin.magicspells.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;

public class CastItem {

	public static final Material MATERIAL_CAST_ITEM_NOTHING = null;

	private Material materialType = Material.AIR;
	private short data = 0;
	private String name = "";
	private int[][] enchants = null;

	public CastItem() {
		// No op
	}

	public CastItem(Material type) {
		this.materialType = type;
	}

	public CastItem(Material type, short data) {
		this.materialType = type;
		if (MagicSpells.ignoreCastItemDurability(this.materialType)) {
			this.data = 0;
		} else {
			this.data = data;
		}
	}

	public CastItem(ItemStack item) {
		if (item == null) {
			this.materialType = Material.AIR;
			this.data = 0;
		} else {
			this.materialType = item.getType();
			if (isCastItemMaterialAirOrNothing(this.materialType) || MagicSpells.ignoreCastItemDurability(this.materialType)) {
				this.data = 0;
			} else {
				this.data = item.getDurability();
			}
			if (!isCastItemMaterialAirOrNothing(this.materialType) && !MagicSpells.ignoreCastItemNames() && item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();
				if (meta.hasDisplayName()) {
					if (MagicSpells.ignoreCastItemNameColors()) {
						this.name = ChatColor.stripColor(meta.getDisplayName());
					} else {
						this.name = meta.getDisplayName();
					}
				}
			}
			if (!isCastItemMaterialAirOrNothing(this.materialType) && !MagicSpells.ignoreCastItemEnchants()) {
				this.enchants = getEnchants(item);
			}
		}
	}

	public CastItem(String string) {
		String s = string;
		if (s.contains("|")) {
			String[] temp = s.split("\\|");
			s = temp[0];
			if (!MagicSpells.ignoreCastItemNames() && temp.length > 1) {
				if (MagicSpells.ignoreCastItemNameColors()) {
					this.name = ChatColor.stripColor(temp[1]);
				} else {
					this.name = temp[1];
				}
			}
		}
		if (s.contains(";")) {
			String[] temp = s.split(";");
			s = temp[0];
			if (!MagicSpells.ignoreCastItemEnchants()) {
				String[] split = temp[1].split("\\+");
				this.enchants = new int[split.length][];
				for (int i = 0; i < this.enchants.length; i++) {
					String[] enchantData = split[i].split("-");
					this.enchants[i] = new int[] { Integer.parseInt(enchantData[0]), Integer.parseInt(enchantData[1]) };
				}
				sortEnchants(this.enchants);
			}
		}
		if (s.contains(":")) {
			String[] split = s.split(":");
			this.materialType = Material.getMaterial(split[0].toUpperCase());
			if (MagicSpells.ignoreCastItemDurability(this.materialType)) {
				this.data = 0;
			} else {
				this.data = Short.parseShort(split[1]);
			}
		} else {
			this.materialType = Material.getMaterial(s.toUpperCase());
			this.data = 0;
		}
	}

	public Material getItemType() {
		return this.materialType;
	}

	public boolean equals(CastItem i) {
		if (i == null) return false;
		if (i.materialType != this.materialType) return false;
		if (i.data != this.data) return false;
		if (!(MagicSpells.ignoreCastItemNames() || i.name.equals(this.name))) return false;
		return MagicSpells.ignoreCastItemEnchants() || compareEnchants(this.enchants, i.enchants);
	}

	public boolean equals(ItemStack i) {
		if (i.getType() != this.materialType) return false;
		if (i.getDurability() != this.data) return false;
		if (!(MagicSpells.ignoreCastItemNames() || namesEqual(i))) return false;
		return MagicSpells.ignoreCastItemEnchants() || compareEnchants(this.enchants, getEnchants(i));
	}

	private boolean namesEqual(ItemStack i) {
		String n = null;
		if (i.hasItemMeta()) {
			ItemMeta meta = i.getItemMeta();
			if (meta.hasDisplayName()) {
				if (MagicSpells.ignoreCastItemNameColors()) {
					n = ChatColor.stripColor(meta.getDisplayName());
				} else {
					n = meta.getDisplayName();
				}
			}
		}
		if (n == null && (this.name == null || this.name.isEmpty())) return true;
		if (n == null || this.name == null) return false;
		return n.equals(this.name);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CastItem) return equals((CastItem)o);
		if (o instanceof ItemStack) return equals((ItemStack)o);
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (this.data == 0) {
			builder.append(this.materialType);
		} else {
			builder.append(this.materialType);
			builder.append(':');
			builder.append(this.data);
		}
		if (this.enchants != null) {
			builder.append(';');
			for (int i = 0; i < this.enchants.length; i++) {
				builder.append(this.enchants[i][0]);
				builder.append('-');
				builder.append(this.enchants[i][1]);
				if (i < this.enchants.length - 1) builder.append('+');
			}
		}
		String s = builder.toString();
		if (this.name != null && !this.name.isEmpty()) s += '|' + this.name;
		return s;
	}

	private int[][] getEnchants(ItemStack item) {
		if (item == null) return null;
		Map<Enchantment, Integer> enchantments = item.getEnchantments();
		if (enchantments == null) return null;
		if (enchantments.isEmpty()) return null;
		int[][] enchants = new int[enchantments.size()][];
		int i = 0;
		for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
			enchants[i] = new int[] { MagicValues.Enchantments.getId(entry.getKey()), entry.getValue() };
			i++;
		}
		sortEnchants(enchants);
		return enchants;
	}

	private static void sortEnchants(int[][] enchants) {
		Arrays.sort(enchants, enchantComparator);
	}

	private static final Comparator<int[]> enchantComparator = (int[] o1, int[] o2) -> o1[0] - o2[0];

	private boolean compareEnchants(int[][] o1, int[][] o2) {
		if (o1 == null && o2 == null) return true;
		if (o1 == null || o2 == null) return false;
		if (o1.length != o2.length) return false;
		for (int i = 0; i < o1.length; i++) {
			if (o1[i][0] != o2[i][0]) return false;
			if (o1[i][1] != o2[i][1]) return false;
		}
		return true;
	}

	private static boolean isCastItemMaterialAirOrNothing(Material material) {
		return material == Material.AIR || material == MATERIAL_CAST_ITEM_NOTHING;
	}

}
