package com.nisovin.magicspells.spells;

import com.nisovin.magicspells.Spell;

import java.util.List;

public interface SpellSupplier {
    List<Spell> getSubSpells();
}
