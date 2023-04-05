package com.nisovin.magicspells.util.wrapper;

import org.bukkit.Material;

public class LegacyBlockUtils implements BlockUtilsWrapper {
    public boolean isPathable(Material material) {
        switch (material) {
            default:
                return false;
            case AIR:
            case SAPLING:
            case WATER:
            case STATIONARY_WATER:
            case POWERED_RAIL:
            case DETECTOR_RAIL:
            case LONG_GRASS:
            case DEAD_BUSH:
            case YELLOW_FLOWER:
            case RED_ROSE:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
            case TORCH:
            case FIRE:
            case REDSTONE_WIRE:
            case CROPS:
            case SIGN_POST:
            case LADDER:
            case RAILS:
            case WALL_SIGN:
            case LEVER:
            case STONE_PLATE:
            case WOOD_PLATE:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
            case STONE_BUTTON:
            case SNOW:
            case SUGAR_CANE_BLOCK:
            case VINE:
            case WATER_LILY:
            case NETHER_STALK:
            case CARPET:
                return true;
        }
    }
}
