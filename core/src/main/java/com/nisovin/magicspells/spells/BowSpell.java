package com.nisovin.magicspells.spells;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityShootBowEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellCastedEvent;

public class BowSpell extends Spell implements SingleSpellSupplier {

	private static BowSpellHandler handler;
	
	private BowSpell thisSpell;

	private String bowName;
	private String spellOnShootName;

	private Subspell spellOnShoot;

	private boolean useBowForce;

	@Override
	public Spell getSubSpell() {
		return spellOnShoot != null ? spellOnShoot.getSpell() : null;
	}

	public BowSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		thisSpell = this;

		bowName = ChatColor.translateAlternateColorCodes('&', getConfigString("bow-name", ""));
		spellOnShootName = getConfigString("spell", "");

		useBowForce = getConfigBoolean("use-bow-force", true);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellOnShoot = new Subspell(spellOnShootName);
		if (!spellOnShoot.process()) {
			MagicSpells.error("BowSpell '" + internalName + "' has an invalid spell defined!");
			spellOnShoot = null;
		}
		
		if (handler == null) handler = new BowSpellHandler();
		handler.registerSpell(this);
	}
	
	@Override
	public void turnOff() {
		super.turnOff();

		if (handler == null) return;
		handler.turnOff();
		handler = null;
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}

	@Override
	public boolean canCastWithItem() {
		return false;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}
	
	private class BowSpellHandler implements Listener {

		private Map<String, BowSpell> spells = new HashMap<>();
		
		BowSpellHandler() {
			registerEvents(this);
		}

		private void registerSpell(BowSpell spell) {
			spells.put(spell.bowName, spell);
		}
		
		@EventHandler
		public void onArrowLaunch(EntityShootBowEvent event) {
			if (event.getEntity().getType() != EntityType.PLAYER) return;
			Player shooter = (Player) event.getEntity();
			ItemStack inHand = shooter.getEquipment().getItemInMainHand();
			if (inHand == null || inHand.getType() != Material.BOW) return;

			String bowName = inHand.getItemMeta().getDisplayName();
			if (bowName.isEmpty()) return;

			Spellbook spellbook = MagicSpells.getSpellbook(shooter);
			BowSpell spell = spells.get(bowName);
			if (spell == null) return;
			if (!spellbook.hasSpell(spell)) return;
			if (!spellbook.canCast(spell)) return;

			if (thisSpell.onCooldown(shooter)) {
				MagicSpells.sendMessage(formatMessage(thisSpell.strOnCooldown, "%c", Math.round(getCooldown(shooter)) + ""), shooter, null);
				return;
			}
			if (!thisSpell.hasReagents(shooter)) {
				MagicSpells.sendMessage(thisSpell.strMissingReagents, shooter, null);
				return;
			}

			SpellCastEvent evt1 = new SpellCastEvent(thisSpell, shooter, SpellCastState.NORMAL, useBowForce ? event.getForce() : 1.0F, null, thisSpell.cooldown, thisSpell.reagents, 0);
			EventUtil.call(evt1);
			if (evt1.isCancelled()) return;
			
			event.setCancelled(true);
			event.getProjectile().remove();
			spell.spellOnShoot.cast(shooter, evt1.getPower());
			thisSpell.setCooldown(shooter, thisSpell.cooldown);
			thisSpell.removeReagents(shooter);
			SpellCastedEvent evt2 = new SpellCastedEvent(thisSpell, shooter, SpellCastState.NORMAL, evt1.getPower(), null, thisSpell.cooldown, thisSpell.reagents, PostCastAction.HANDLE_NORMALLY);
			EventUtil.call(evt2);
		}

		private void turnOff() {
			unregisterEvents(this);
			spells.clear();
		}
		
	}
	
}
