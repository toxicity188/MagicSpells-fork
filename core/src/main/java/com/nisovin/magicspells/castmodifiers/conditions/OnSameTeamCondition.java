package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnSameTeamCondition extends Condition {
	
	@Override
	public boolean setVar(String var) {
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		if (target instanceof Player && livingEntity instanceof Player) {
			Team team1 = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam((Player) livingEntity);
			Team team2 = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam((Player) target);
			return team1 != null && team2 != null && team1.equals(team2);
		}
		return false;
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}
	
}
