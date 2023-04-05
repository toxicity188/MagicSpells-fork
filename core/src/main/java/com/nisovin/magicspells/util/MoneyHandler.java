package com.nisovin.magicspells.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;
import com.nisovin.magicspells.util.compat.CompatBasics;

public class MoneyHandler {

	private Economy economy;

	public MoneyHandler() {
		RegisteredServiceProvider<Economy> provider = CompatBasics.getServiceProvider(Economy.class);
		if (provider != null) this.economy = provider.getProvider();
	}

	public boolean hasMoney(Player player, float money) {
		if (this.economy == null) return false;
		return economy.has(player, money);
	}

	public void removeMoney(Player player, float money) {
		if (this.economy == null) return;
		economy.withdrawPlayer(player, money);
	}

	public void addMoney(Player player, float money) {
		if (this.economy == null) return;
		economy.depositPlayer(player, money);
	}

	public double checkMoney(Player player) {
		if (this.economy == null) return 0;
		return economy.getBalance(player);
	}

}
