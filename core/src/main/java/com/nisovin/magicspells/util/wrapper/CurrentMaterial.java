package com.nisovin.magicspells.util.wrapper;

import lombok.Getter;
import org.bukkit.Material;

@Getter
public final class CurrentMaterial implements IMaterialGetter {
    private final Material craftingTable = Material.valueOf("CRAFTING_TABLE");
    private final Material enchantingTable = Material.valueOf("ENCHANTING_TABLE");
    private final Material caveAir = Material.valueOf("CAVE_AIR");
    private final Material voidAir = Material.valueOf("VOID_AIR");
    private final Material tallGrass = Material.valueOf("TALL_GRASS");
    private final Material woodenShovel = Material.valueOf("WOODEN_SHOVEL");
    private final Material woodenHoe = Material.valueOf("WOODEN_HOE");
    private final Material woodenAxe = Material.valueOf("WOODEN_AXE");
    private final Material woodenPickaxe = Material.valueOf("WOODEN_PICKAXE");
    private final Material woodenSword = Material.valueOf("WOODEN_SWORD");
    private final Material log = Material.valueOf("LEGACY_LOG");
    private final Material sapling = Material.valueOf("LEGACY_SAPLING");
    private final Material leaves = Material.valueOf("LEGACY_LEAVES");
    private final Material wood = Material.valueOf("LEGACY_WOOD");
    private final Material wool = Material.valueOf("LEGACY_WOOL");
    private final Material inkSack = Material.valueOf("LEGACY_INK_SACK");
    private final Material grass = Material.valueOf("GRASS_BLOCK");
    private final Material lilyPad = Material.valueOf("LILY_PAD");
    private final Material firework = Material.valueOf("FIREWORK_ROCKET");
    private final Material ironBarding = Material.valueOf("LEGACY_IRON_BARDING");
    private final Material goldBarding = Material.valueOf("LEGACY_GOLD_BARDING");
    private final Material diamondBarding = Material.valueOf("LEGACY_DIAMOND_BARDING");
}
