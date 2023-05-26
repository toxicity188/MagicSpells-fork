package com.nisovin.magicspells.commands;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.util.regex.Pattern;

import com.nisovin.magicspells.power.Power;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.command.Command;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.command.BlockCommandSender;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.variables.PlayerStringVariable;

public class CastCommand implements CommandExecutor, TabCompleter {

    private static final Pattern LOOSE_PLAYER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("^[^,]+,-?[0-9.]+,-?[0-9.]+,-?[0-9.]+(,-?[0-9.]+,-?[0-9.]+)?$");

    private MagicSpells plugin;
    private boolean enableTabComplete;

    public CastCommand(MagicSpells plugin, boolean enableTabComplete) {
        this.plugin = plugin;
        this.enableTabComplete = enableTabComplete;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String [] args) {
        try {
            if (!command.getName().equalsIgnoreCase("magicspellcast")) return false;
            args = Util.splitParams(args);

            // command with no arguments
            if (args == null || args.length == 0) {
                if (sender instanceof Player) MagicSpells.sendMessage(MagicSpells.getStrCastUsage(), (Player) sender, MagicSpells.NULL_ARGS);
                else sender.sendMessage(MagicSpells.getTextColor() + MagicSpells.getStrCastUsage());
                return true;
            }

            // forcecast
            if (Perm.FORCECAST.has(sender) && args[0].equals("forcecast")) {
                if (args.length < 3) {
                    sender.sendMessage(MagicSpells.getTextColor() + "The correct syntax is /c forcecast <playerName/mobUUID> <spellInGameName> [spellArgs]");
                    return true;
                }

                Player target = PlayerNameUtils.getPlayer(args[1]);
                Entity targetEntity = null;

                // if player is null, check for entity uuid
                if (target == null) {
                    targetEntity = Bukkit.getEntity(UUID.fromString(args[1]));
                    if (targetEntity == null || !(targetEntity instanceof LivingEntity)) {
                        sender.sendMessage(MagicSpells.getTextColor() + "No matching player or living entity found!");
                        return true;
                    }
                }

                Spell spell = MagicSpells.getSpellByInGameName(args[2]);
                // spell not found
                if (spell == null) {
                    sender.sendMessage(MagicSpells.getTextColor() + "Spell with that in-game name doesn't exist!");
                    return true;
                }

                String[] spellArgs = null;
                if (args.length > 3) spellArgs = Arrays.copyOfRange(args, 3, args.length);
                if (target != null) {
                    spell.cast(target, spellArgs);
                    if (MagicSpells.isDebug()) sender.sendMessage(MagicSpells.getTextColor() + "Player " + target.getName() + " forced to cast " + spell.getName());
                } else if (targetEntity != null) {
                    spell.cast((LivingEntity) targetEntity, spellArgs);
                    if (MagicSpells.isDebug()) sender.sendMessage(MagicSpells.getTextColor() + "LivingEntity " + targetEntity.getName() + " forced to cast " + spell.getName());
                }

                return true;
            }

            // castat
            if (Perm.CAST_AT.has(sender) && args[0].equals("castat")) {
                if (args.length < 3) {
                    sender.sendMessage(MagicSpells.getTextColor() + "The correct syntax is /c castat <playerName/mobUUID> <spellInGameName> [power]");
                    return true;
                }

                // castat <playerName/mobUUID> <spellInGameName> [power]
                Player target = Bukkit.getServer().getPlayer(args[1]);
                Entity targetEntity = null;

                // if player is null, check for entity uuid
                if (target == null) {
                    targetEntity = Bukkit.getEntity(UUID.fromString(args[1]));
                    if (targetEntity == null || !(targetEntity instanceof LivingEntity)) {
                        sender.sendMessage(MagicSpells.getTextColor() + "No matching player or living entity found!");
                        return true;
                    }
                }

                Spell spell = MagicSpells.getSpellByInGameName(args[2]);
                TargetedEntitySpell tes = null;
                TargetedLocationSpell tls = null;

                if (spell instanceof TargetedEntitySpell) tes = (TargetedEntitySpell) spell;
                else if (spell instanceof TargetedLocationSpell) tls = (TargetedLocationSpell) spell;
                else {
                    sender.sendMessage(MagicSpells.getTextColor() + "You did not specify a targeted entity or targeted location spell");
                    return true;
                }

                Power cPower = new Power(1F);
                if (args.length == 4) cPower = new Power(Float.parseFloat(args[3]));

                if (tes != null) {
                    if (target != null) tes.castAtEntity(target, cPower);
                    else if (targetEntity != null) tes.castAtEntity((LivingEntity) targetEntity, cPower);
                } else if (tls != null) {
                    if (target != null) tls.castAtLocation(target.getLocation(), cPower);
                    else if (targetEntity != null) tls.castAtLocation(targetEntity.getLocation(), cPower);
                }

                return true;
            }

            // reload
            if (Perm.RELOAD.has(sender) && args[0].equals("reload")) {
                // reload the plugin if no player name is specified
                if (args.length == 1) {
                    plugin.unload();
                    plugin.load();
                    sender.sendMessage(MagicSpells.getTextColor() + "MagicSpells config reloaded.");
                    return true;
                }

                // reload player's spellbook
                List<Player> players = plugin.getServer().matchPlayer(args[1]);
                if (players.size() != 1) {
                    sender.sendMessage(MagicSpells.getTextColor() + "Player not found.");
                    return true;
                }

                Player player = players.get(0);
                MagicSpells.getSpellbooks().put(player.getName(), new Spellbook(player, plugin));
                sender.sendMessage(MagicSpells.getTextColor() + player.getName() + "'s spellbook reloaded.");

                return true;
            }

            // resetcd
            if (Perm.RESET_COOLDOWN.has(sender) && args[0].equals("resetcd")) {
                Player p = null;
                if (args.length > 1) {
                    // check for player name
                    p = PlayerNameUtils.getPlayer(args[1]);
                    if (p == null) {
                        sender.sendMessage(MagicSpells.getTextColor() + "No matching player found");
                        return true;
                    }
                }

                for (Spell spell : MagicSpells.getSpells().values()) {
                    if (p != null) spell.setCooldown(p, 0);
                    else spell.getCooldowns().clear();
                }
                sender.sendMessage(MagicSpells.getTextColor() + "Cooldowns reset" + (p != null ? " for " + p.getName() : ""));

                return true;
            }

            // resetmana
            if (Perm.RESET_MANA.has(sender) && args[0].equals("resetmana") && args.length > 1 && MagicSpells.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                MagicSpells.getManaHandler().createManaBar(p);
                sender.sendMessage(MagicSpells.getTextColor() + p.getName() + "'s mana reset.");

                return true;
            }

            // updatemanarank
            if (Perm.UPDATE_MANA_RANK.has(sender) && args[0].equals("updatemanarank") && args.length > 1 && MagicSpells.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                boolean updated = MagicSpells.getManaHandler().updateManaRankIfNecessary(p);
                MagicSpells.getManaHandler().showMana(p);

                if (updated) sender.sendMessage(MagicSpells.getTextColor() + p.getName() + "'s mana rank updated.");
                else sender.sendMessage(MagicSpells.getTextColor() + p.getName() + "'s mana rank already correct.");

                return true;
            }

            // setmaxmana
            if (Perm.SET_MAX_MANA.has(sender) && args[0].equalsIgnoreCase("setmaxmana") && args.length == 3 && MagicSpells.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                int amt = Integer.parseInt(args[2]);
                MagicSpells.getManaHandler().setMaxMana(p, amt);
                sender.sendMessage(MagicSpells.getTextColor() + p.getName() + "'s max mana set to " + amt + '.');

                return true;
            }

            // modifymana
            if (Perm.MODIFY_MANA.has(sender) && args[0].equalsIgnoreCase("modifymana") && args.length == 3 && MagicSpells.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                int amt = Integer.parseInt(args[2]);
                MagicSpells.getManaHandler().addMana(p, amt, ManaChangeReason.OTHER);
                sender.sendMessage(MagicSpells.getTextColor() + p.getName() + "'s mana modified by " + amt + '.');

                return true;
            }

            // setmana
            if (Perm.SET_MANA.has(sender) && args[0].equalsIgnoreCase("setmana") && args.length == 3 && MagicSpells.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                int amt = Integer.parseInt(args[2]);
                MagicSpells.getManaHandler().setMana(p, amt, ManaChangeReason.OTHER);
                sender.sendMessage(MagicSpells.getTextColor() + p.getName() + "'s mana set to " + amt + '.');

                return true;
            }

            if (Perm.SHOW_VARIABLE.has(sender) && args[0].equals("showvariable")) {
                if (args.length != 3) {
                    sender.sendMessage(MagicSpells.getTextColor() + "The correct syntax is /c showvariable <variable> <player>");
                    return true;
                }

                String var = args[1];
                String player = args[2];
                sender.sendMessage(MagicSpells.getTextColor() + MagicSpells.getVariableManager().getStringValue(var, player));

                return true;
            }

            // modifyvariable <variable> <player> (+|-|*|/|=)<value>
            if (Perm.MODIFY_VARIABLE.has(sender) && args[0].equals("modifyvariable")) {
                if (args.length != 4) {
                    sender.sendMessage(MagicSpells.getTextColor() + "The correct syntax is /c modifyvariable <variable> <player> <[operator]value>");
                    return true;
                }

                String var = args[1];
                String player = args[2];
                boolean set = false;
                boolean multiply = false;
                boolean divide = false;
                double num = 0;
                String numString = args[3];
                String valueString = args[3];
                Variable variable = MagicSpells.getVariableManager().getVariable(var);

                // Possible operations + - = * /

                if (numString.startsWith("*")) {
                    multiply = true;
                    numString = numString.substring(1);
                } else if (numString.startsWith("/")) {
                    divide = true;
                    numString = numString.substring(1);
                } else if (numString.startsWith("=")) {
                    set = true;
                    numString = numString.substring(1);
                    valueString = valueString.substring(1);
                } else if (numString.startsWith("+")) {
                    numString = numString.substring(1);
                }

                if (!RegexUtil.matches(RegexUtil.DOUBLE_PATTERN, numString)) {
                    boolean negate = false;
                    if (numString.startsWith("-")) {
                        negate = true;
                        numString = numString.substring(1);
                    }
                    String targetPlayerName = player;
                    if (numString.contains(":")) {
                        String[] targetVarData = numString.split(":");
                        targetPlayerName = targetVarData[0];
                        numString = targetVarData[1];
                    }
                    num = MagicSpells.getVariableManager().getValue(numString, PlayerNameUtils.getPlayer(targetPlayerName));
                    if (negate) num *= -1;
                } else num = Double.parseDouble(numString);

                if (multiply) MagicSpells.getVariableManager().multiplyBy(var, PlayerNameUtils.getPlayer(player), num);
                else if (divide) MagicSpells.getVariableManager().divideBy(var, PlayerNameUtils.getPlayer(player), num);
                else if (set) {
                    if (variable instanceof PlayerStringVariable) MagicSpells.getVariableManager().set(var, player, valueString);
                    else MagicSpells.getVariableManager().set(var, player, num);
                }
                else MagicSpells.getVariableManager().modify(var, player, num);

                return true;
            }

            // magicitem
            if (Perm.MAGICITEM.has(sender) && args[0].equals("magicitem")) {
                if (args.length < 3) {
                    sender.sendMessage(MagicSpells.getTextColor() + "The correct syntax is /c magicitem <playerName> <itemName> [amount]");
                    return true;
                }

                Player target = Bukkit.getServer().getPlayer(args[1]);
                if (target == null || !(target instanceof InventoryHolder)) {
                    sender.sendMessage(MagicSpells.getTextColor() + "Player not found.");
                    return true;
                }

                ItemStack item = Util.getItemStackFromString(args[2]);
                if (item == null) {
                    sender.sendMessage(MagicSpells.getTextColor() + "Item not found.");
                    return true;
                }

                // set item amount
                if (args.length > 3 && RegexUtil.matches(RegexUtil.SIMPLE_INT_PATTERN, args[3])) item.setAmount(Integer.parseInt(args[3]));
                target.getInventory().addItem(item);

                return true;
            }

            // download
            if (Perm.DOWNLOAD.has(sender) && args[0].equals("download")) {
                if (args.length != 3) {
                    sender.sendMessage(MagicSpells.getTextColor() + "The correct syntax is /c download <fileName> <downloadLink>");
                    return true;
                }

                File file = new File(plugin.getDataFolder(), "spells-" + args[1] + ".yml");
                if (file.exists()) {
                    sender.sendMessage(MagicSpells.getTextColor() + "ERROR: The file spells-" + args[1] + ".yml already exists!");
                    return true;
                }

                boolean downloaded = Util.downloadFile(args[2], file);
                if (downloaded) sender.sendMessage(MagicSpells.getTextColor() + "SUCCESS! You will need to do a /cast reload to load the new spells.");
                else sender.sendMessage(MagicSpells.getTextColor() + "ERROR: The file could not be downloaded.");

                return true;
            }

            // update
            if (Perm.UPDATE.has(sender) && args[0].equals("update")) {
                if (args.length != 3) {
                    sender.sendMessage(MagicSpells.getTextColor() + "The correct syntax is /c update <fileName> <downloadLink>");
                    return true;
                }

                File file = new File(plugin.getDataFolder(), "update-" + args[1] + ".yml");
                boolean downloaded = Util.downloadFile(args[2], file);
                boolean abort = false;
                if (!downloaded) {
                    sender.sendMessage(MagicSpells.getTextColor() + "Update file failed to download.");
                    return true;
                }

                sender.sendMessage(MagicSpells.getTextColor() + "Update file successfully downloaded.");

                // Delete the existing file
                File old = new File(plugin.getDataFolder(), args[1] + ".yml");
                if (old.exists()) {
                    boolean deleteSuccess = old.delete();
                    if (deleteSuccess) sender.sendMessage(MagicSpells.getTextColor() + "Old file successfully deleted.");
                    else {
                        sender.sendMessage(MagicSpells.getTextColor() + "Old file could not be deleted.");
                        sender.sendMessage(MagicSpells.getTextColor() + "Aborting update, please delete the update file: " + file.getName());
                        abort = true;
                    }
                } else sender.sendMessage(MagicSpells.getTextColor() + "There was no old file to delete.");

                if (!abort) {
                    // Rename the update to the original file's name
                    boolean renamingSuccess = file.renameTo(new File(plugin.getDataFolder(), args[1] + ".yml"));
                    if (renamingSuccess) {
                        sender.sendMessage(MagicSpells.getTextColor() + "Successfully renamed the update file to " + args[1] + ".yml");
                        sender.sendMessage(MagicSpells.getTextColor() + "You will need to do a /cast reload to load the update.");
                    } else sender.sendMessage(MagicSpells.getTextColor() + "Failed to rename the update file, update failed");
                }

                return true;
            }

            // saveskin
            if (Perm.SAVESKIN.has(sender) && args[0].equals("saveskin") && args.length == 3) {
                Player player = PlayerNameUtils.getPlayerExact(args[1]);
                if (player == null) return true;

                MagicSpells.getVolatileCodeHandler().saveSkinData(player, args[2]);
                sender.sendMessage(MagicSpells.getTextColor() + "Skin data for player " + player.getName() + " saved as " + args[2]);

                return true;
            }

            // profilereport
            if (Perm.PROFILE.has(sender) && args[0].equals("profilereport")) {
                sender.sendMessage(MagicSpells.getTextColor() + "Creating profiling report");
                MagicSpells.profilingReport();

                return true;
            }

            // debug
            if (Perm.DEBUG.has(sender) && args[0].equals("debug")) {
                MagicSpells.setDebug(!MagicSpells.isDebug());
                sender.sendMessage(MagicSpells.getTextColor() + "[MagicSpells]: Debug - " + (MagicSpells.isDebug() ? "enabled" : "disabled"));

                return true;
            }

            // cast spell
            if (sender instanceof LivingEntity) {
                LivingEntity caster = (LivingEntity) sender;
                Spell spell = MagicSpells.getSpellByInGameName(args[0]);

                // if caster is a player
                if (caster instanceof Player) {
                    Spellbook spellbook = MagicSpells.getSpellbook((Player) caster);
                    if (spell != null && (!spell.isHelperSpell() || caster.isOp()) && spell.canCastByCommand() && spellbook.hasSpell(spell)) {
                        if (!spell.isValidItemForCastCommand(caster.getEquipment().getItemInMainHand())) {
                            MagicSpells.sendMessage(spell.getStrWrongCastItem(), caster, null);
                            return true;
                        }

                        String[] spellArgs = null;
                        if (args.length > 1) {
                            spellArgs = new String[args.length - 1];
                            System.arraycopy(args, 1, spellArgs, 0, args.length - 1);
                        }
                        spell.cast(caster, spellArgs);

                        return true;
                    }
                    MagicSpells.sendMessage(MagicSpells.getStrUnknownSpell(), caster, null);

                    return true;
                }

                // if caster is a living entity
                if (spell != null && (!spell.isHelperSpell() || caster.isOp()) && spell.canCastByCommand()) {
                    if (!spell.isValidItemForCastCommand(caster.getEquipment().getItemInMainHand())) {
                        MagicSpells.sendMessage(spell.getStrWrongCastItem(), caster, null);
                        return true;
                    }

                    String[] spellArgs = null;
                    if (args.length > 1) {
                        spellArgs = new String[args.length - 1];
                        System.arraycopy(args, 1, spellArgs, 0, args.length - 1);
                    }
                    spell.cast(caster, spellArgs);

                    return true;
                }

                MagicSpells.sendMessage(MagicSpells.getStrUnknownSpell(), caster, null);

                return true;
            }

            // invalid spell
            Spell spell = MagicSpells.getSpellNames().get(args[0].toLowerCase());
            if (spell == null) {
                sender.sendMessage(MagicSpells.getTextColor() + "Unknown spell.");
                return true;
            }

            String[] spellArgs = null;
            if (args.length > 1) {
                spellArgs = new String[args.length - 1];
                System.arraycopy(args, 1, spellArgs, 0, args.length - 1);
            }

            boolean casted = false;
            if (sender instanceof BlockCommandSender && spell instanceof TargetedLocationSpell) {
                Location loc = ((BlockCommandSender) sender).getBlock().getLocation().add(0.5, 0.5, 0.5);
                if (spellArgs != null && spellArgs.length >= 3) {
                    try {
                        int x = Integer.parseInt(spellArgs[0]);
                        int y = Integer.parseInt(spellArgs[1]);
                        int z = Integer.parseInt(spellArgs[2]);
                        float yaw = 0;
                        float pitch = 0;
                        if (spellArgs.length > 3) yaw = Float.parseFloat(spellArgs[3]);
                        if (spellArgs.length > 4) pitch = Float.parseFloat(spellArgs[4]);
                        loc.add(x, y, z);
                        loc.setYaw(yaw);
                        loc.setPitch(pitch);
                    } catch (NumberFormatException e) {
                        DebugHandler.debugNumberFormat(e);
                    }
                }
                ((TargetedLocationSpell) spell).castAtLocation(loc, new Power(1));
                casted = true;
            }

            if (casted) return true;

            // On to trying to handle as a non player entity, only for targeted location spells
            if (sender instanceof Entity && spell instanceof TargetedLocationSpell) {
                Entity senderEntity = (Entity) sender;
                Location loc = senderEntity.getLocation();
                if (spellArgs != null && spellArgs.length >= 3) {
                    try {
                        int x = Integer.parseInt(spellArgs[0]);
                        int y = Integer.parseInt(spellArgs[1]);
                        int z = Integer.parseInt(spellArgs[2]);
                        float yaw = 0;
                        float pitch = 0;
                        if (spellArgs.length > 3) yaw = Float.parseFloat(spellArgs[3]);
                        if (spellArgs.length > 4) pitch = Float.parseFloat(spellArgs[4]);
                        loc.add(x, y, z);
                        loc.setYaw(yaw);
                        loc.setPitch(pitch);
                    } catch (NumberFormatException e) {
                        DebugHandler.debugNumberFormat(e);
                    }
                }
                ((TargetedLocationSpell) spell).castAtLocation(loc, new Power(1.0F));
                casted = true;
            }

            if (casted) return true;

            boolean ok = spell.castFromConsole(sender, spellArgs);
            if (!ok) return true;

            if ((spell instanceof TargetedEntitySpell || spell instanceof TargetedLocationSpell) && spellArgs != null && spellArgs.length == 1 && RegexUtil.matches(LOOSE_PLAYER_NAME_PATTERN, spellArgs[0])) {
                Player target = PlayerNameUtils.getPlayer(spellArgs[0]);
                if (target == null) {
                    sender.sendMessage(MagicSpells.getTextColor() + "Invalid target.");
                    return true;
                }

                if (spell instanceof TargetedEntitySpell) ok = ((TargetedEntitySpell) spell).castAtEntity(target, new Power(1));
                else if (spell instanceof TargetedLocationSpell) ok = ((TargetedLocationSpell) spell).castAtLocation(target.getLocation(), new Power(1));

                if (ok) sender.sendMessage(MagicSpells.getTextColor() + "Spell casted!");
                else sender.sendMessage(MagicSpells.getTextColor() + "Spell failed, probably can't be cast from console.");

                return true;
            }

            if (spell instanceof TargetedLocationSpell && spellArgs != null && spellArgs.length == 1 && RegexUtil.matches(LOCATION_PATTERN, spellArgs[0])) {
                String[] locData = spellArgs[0].split(",");
                World world = Bukkit.getWorld(locData[0]);
                if (world == null) {
                    sender.sendMessage(MagicSpells.getTextColor() + "No such world.");
                    return true;
                }

                Location loc = new Location(world, Float.parseFloat(locData[1]), Float.parseFloat(locData[2]), Float.parseFloat(locData[3]));
                if (locData.length > 4) loc.setYaw(Float.parseFloat(locData[4]));
                if (locData.length > 5) loc.setPitch(Float.parseFloat(locData[5]));
                ok = ((TargetedLocationSpell) spell).castAtLocation(loc, new Power(1));

                if (ok) sender.sendMessage(MagicSpells.getTextColor() + "Spell casted!");
                else sender.sendMessage(MagicSpells.getTextColor() + "Spell failed, probably can't be cast from console.");

                return true;
            }

            sender.sendMessage(MagicSpells.getTextColor() + "Cannot cast that spell from console.");

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
