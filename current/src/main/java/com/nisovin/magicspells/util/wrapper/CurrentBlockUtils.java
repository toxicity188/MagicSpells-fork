package com.nisovin.magicspells.util.wrapper;

import org.bukkit.Material;

public class CurrentBlockUtils implements BlockUtilsWrapper {

    @Override
    public boolean isPathable(Material mat) {
        if (mat.name().contains("SIGN")) return true;
        switch (mat) {
            default:
                return false;
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
            case OAK_SAPLING:
            case ACACIA_SAPLING:
            case JUNGLE_SAPLING:
            case SPRUCE_SAPLING:
            case DARK_OAK_SAPLING:
            case BIRCH_SAPLING:
            case WATER:
            case TALL_GRASS:
            case LARGE_FERN:
            case GRASS:
            case DEAD_BUSH:
            case FERN:
            case SEAGRASS:
            case TALL_SEAGRASS:
            case LILY_PAD:
            case DANDELION:
            case POPPY:
            case BLUE_ORCHID:
            case ALLIUM:
            case AZURE_BLUET:
            case ORANGE_TULIP:
            case PINK_TULIP:
            case RED_TULIP:
            case WHITE_TULIP:
            case OXEYE_DAISY:
            case SUNFLOWER:
            case LILAC:
            case PEONY:
            case ROSE_BUSH:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
            case TORCH:
            case FIRE:
            case REDSTONE_WIRE:
            case WHEAT:
            case LADDER:
            case RAIL:
            case ACTIVATOR_RAIL:
            case DETECTOR_RAIL:
            case POWERED_RAIL:
            case LEVER:
            case REDSTONE_TORCH:
            case STONE_BUTTON:
            case OAK_BUTTON:
            case ACACIA_BUTTON:
            case JUNGLE_BUTTON:
            case SPRUCE_BUTTON:
            case DARK_OAK_BUTTON:
            case BIRCH_BUTTON:
            case SNOW:
            case SUGAR_CANE:
            case VINE:
            case NETHER_WART:
            case BLACK_CARPET:
            case BLUE_CARPET:
            case CYAN_CARPET:
            case BROWN_CARPET:
            case GRAY_CARPET:
            case GREEN_CARPET:
            case LIGHT_BLUE_CARPET:
            case LIGHT_GRAY_CARPET:
            case LIME_CARPET:
            case MAGENTA_CARPET:
            case ORANGE_CARPET:
            case PINK_CARPET:
            case PURPLE_CARPET:
            case RED_CARPET:
            case WHITE_CARPET:
            case YELLOW_CARPET:
            case ACACIA_PRESSURE_PLATE:
            case BIRCH_PRESSURE_PLATE:
            case DARK_OAK_PRESSURE_PLATE:
            case HEAVY_WEIGHTED_PRESSURE_PLATE:
            case JUNGLE_PRESSURE_PLATE:
            case LIGHT_WEIGHTED_PRESSURE_PLATE:
            case OAK_PRESSURE_PLATE:
            case SPRUCE_PRESSURE_PLATE:
            case STONE_PRESSURE_PLATE:
            case TUBE_CORAL:
            case BRAIN_CORAL:
            case BUBBLE_CORAL:
            case FIRE_CORAL:
            case HORN_CORAL:
            case DEAD_TUBE_CORAL:
            case DEAD_BRAIN_CORAL:
            case DEAD_BUBBLE_CORAL:
            case DEAD_FIRE_CORAL:
            case DEAD_HORN_CORAL:
            case TUBE_CORAL_FAN:
            case BRAIN_CORAL_FAN:
            case BUBBLE_CORAL_FAN:
            case FIRE_CORAL_FAN:
            case HORN_CORAL_FAN:
            case DEAD_TUBE_CORAL_FAN:
            case DEAD_BRAIN_CORAL_FAN:
            case DEAD_BUBBLE_CORAL_FAN:
            case DEAD_FIRE_CORAL_FAN:
            case DEAD_HORN_CORAL_FAN:
            case TUBE_CORAL_WALL_FAN:
            case BRAIN_CORAL_WALL_FAN:
            case BUBBLE_CORAL_WALL_FAN:
            case FIRE_CORAL_WALL_FAN:
            case HORN_CORAL_WALL_FAN:
            case DEAD_TUBE_CORAL_WALL_FAN:
            case DEAD_BRAIN_CORAL_WALL_FAN:
            case DEAD_BUBBLE_CORAL_WALL_FAN:
            case DEAD_FIRE_CORAL_WALL_FAN:
            case DEAD_HORN_CORAL_WALL_FAN:
                return true;
        }
    }
}
