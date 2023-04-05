package com.nisovin.magicspells.volatilecode

import com.nisovin.magicspells.MagicSpells
import org.bukkit.Bukkit

object ManagerVolatile {

    fun constructVolatileCodeHandler(): VolatileCodeHandle {
        try {
            val nmsPackage = Bukkit.getServer().javaClass.getPackage().name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[3]
            val volatileCode = Class.forName("com.nisovin.magicspells.volatilecode." + nmsPackage + ".VolatileCode" + nmsPackage.replace("v", ""))

            MagicSpells.log("Found volatile code handler for $nmsPackage.")
            return volatileCode.newInstance() as VolatileCodeHandle
        } catch (ex: Exception) {
            // No volatile code handler found
        }

        return VolatileCodeDisabled()
    }
}