package com.nisovin.magicspells.util.wrapper;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public final class LegacyMaterial implements IMaterialGetter {
    private final Material craftingTable = Material.valueOf("WORKBENCH");
    private final Material enchantingTable = Material.valueOf("ENCHANTMENT_TABLE");
    private final Material caveAir = Material.valueOf("AIR");
    private final Material voidAir = Material.valueOf("AIR");
    private final Material tallGrass = Material.valueOf("LONG_GRASS");
    private final Material woodenShovel = Material.valueOf("WOOD_SPADE");
    private final Material woodenHoe = Material.valueOf("WOOD_HOE");
    private final Material woodenAxe = Material.valueOf("WOOD_AXE");
    private final Material woodenPickaxe = Material.valueOf("WOOD_PICKAXE");
    private final Material woodenSword = Material.valueOf("WOOD_SWORD");
    private final Material log = Material.valueOf("LOG");
    private final Material sapling = Material.valueOf("SAPLING");
    private final Material leaves = Material.valueOf("LEAVES");
    private final Material wood = Material.valueOf("WOOD");
    private final Material wool = Material.valueOf("WOOL");
    private final Material inkSack = Material.valueOf("INK_SACK");
    private final Material grass = Material.valueOf("GRASS");
    private final Material lilyPad = Material.valueOf("WATER_LILY");
    private final Material firework = Material.valueOf("FIREWORK_CHARGE");
    private final Material ironBarding = Material.valueOf("IRON_BARDING");
    private final Material goldBarding = Material.valueOf("GOLD_BARDING");
    private final Material diamondBarding = Material.valueOf("DIAMOND_BARDING");
}
