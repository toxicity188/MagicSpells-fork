package com.nisovin.magicspells.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;

public class ManaCommand implements CommandExecutor, TabCompleter {

    private MagicSpells plugin;
    private boolean enableTabComplete;

    public ManaCommand(MagicSpells plugin, boolean enableTabComplete) {
        this.plugin = plugin;
        this.enableTabComplete = enableTabComplete;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String [] args) {
        try {
            if (!command.getName().equalsIgnoreCase("magicspellmana")) return false;
            if (!MagicSpells.enableManaBars() || !(sender instanceof Player)) return true;

            Player player = (Player) sender;
            MagicSpells.getManaHandler().showMana(player, true);

            return true;

        } catch (Exception ex) {
            MagicSpells.handleException(ex);
            sender.sendMessage(ChatColor.RED + "An error has occured.");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!enableTabComplete || !(sender instanceof Player)) return null;

        Spellbook spellbook = MagicSpells.getSpellbook((Player) sender);
        String partial = Util.arrayJoin(args, ' ');
        return spellbook.tabComplete(partial);
    }

}
