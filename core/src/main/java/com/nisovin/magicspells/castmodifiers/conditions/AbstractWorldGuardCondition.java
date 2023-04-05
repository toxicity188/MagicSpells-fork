package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;


import com.nisovin.magicspells.castmodifiers.Condition;
import org.codemc.worldguardwrapper.WorldGuardWrapper;
import org.codemc.worldguardwrapper.region.IWrappedRegion;

import java.util.Set;

public abstract class AbstractWorldGuardCondition extends Condition {

	protected WorldGuardWrapper worldGuard = WorldGuardWrapper.getInstance();

	protected boolean worldGuardEnabled() {
		return worldGuard.getWorldGuardPlugin() != null && worldGuard.getWorldGuardPlugin().isEnabled();
	}

	protected IWrappedRegion getTopPriorityRegion(Location loc) {
		Set<IWrappedRegion> regions = worldGuard.getRegions(loc);
		IWrappedRegion topRegion = null;
		int topPriority = Integer.MIN_VALUE;
		for (IWrappedRegion region: regions) {
			if (region.getPriority() > topPriority) {
				topRegion = region;
				topPriority = region.getPriority();
			}
		}
		return topRegion;
	}
	
}
