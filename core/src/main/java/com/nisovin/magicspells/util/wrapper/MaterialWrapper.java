package com.nisovin.magicspells.util.wrapper;

import com.nisovin.magicspells.MagicSpells;
import lombok.Getter;
import org.bukkit.Bukkit;

public class MaterialWrapper {
    private static final Version VERSION = parseVersion();
    private static Version parseVersion() {
        try {
            int version = Integer.parseInt(Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].split("_")[1]);
            if (version <= 12) {
                return Version.LEGACY;
            } else {
                return Version.CURRENT;
            }
        } catch (Exception e) {
            MagicSpells.log("unable to load wrapped material.");
        }
        return Version.UNKNOWN;
    }
    public static Version getVersion() {
        return VERSION;
    }
    public enum Version {
        LEGACY,
        CURRENT,
        UNKNOWN
    }
}
