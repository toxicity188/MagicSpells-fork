package com.nisovin.magicspells.spells;

import com.nisovin.magicspells.Spell;

import java.util.Collections;
import java.util.List;

public interface SingleSpellSupplier extends SpellSupplier {
    Spell getSubSpell();

    @Override
    default List<Spell> getSubSpells() {
        Spell spell = getSubSpell();
        return spell != null ? Collections.singletonList(spell) : Collections.emptyList();
    }
}
