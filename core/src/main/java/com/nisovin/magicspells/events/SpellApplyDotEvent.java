package com.nisovin.magicspells.events;

import com.nisovin.magicspells.Spell;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@AllArgsConstructor
public class SpellApplyDotEvent extends Event implements IMagicSpellsCompatEvent {
    @Getter
    private static final HandlerList handlerList = new HandlerList();

    @Getter
    @Setter
    private Spell spell;

    @Getter
    @Setter
    private LivingEntity caster, target;

    @Getter
    @Setter
    private double damage;
    @Getter
    @Setter
    private int delay, interval, duration;

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
