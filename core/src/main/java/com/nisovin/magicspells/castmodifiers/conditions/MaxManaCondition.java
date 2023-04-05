package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.mana.ManaHandler;

public class MaxManaCondition extends OperatorCondition {

    private ManaHandler mana;

    private int amount = 0;

    @Override
    public boolean setVar(String var) {
        if (var.length() < 2) {
            return false;
        }

        super.setVar(var);

        mana = MagicSpells.getManaHandler();
        if (mana == null) return false;

        try {
            amount = Integer.parseInt(var.substring(1));
            return true;
        } catch (NumberFormatException e) {
            DebugHandler.debugNumberFormat(e);
            return false;
        }
    }

    @Override
    public boolean check(LivingEntity livingEntity) {
        return maxMana(livingEntity);
    }

    @Override
    public boolean check(LivingEntity livingEntity, LivingEntity target) {
        return maxMana(target);
    }

    @Override
    public boolean check(LivingEntity livingEntity, Location location) {
        return false;
    }

    private boolean maxMana(LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player)) return false;
        if (equals) return mana.getMaxMana((Player) livingEntity) == amount;
        else if (moreThan) return mana.getMaxMana((Player) livingEntity) > amount;
        else if (lessThan) return mana.getMaxMana((Player) livingEntity) < amount;
        return false;
    }

}