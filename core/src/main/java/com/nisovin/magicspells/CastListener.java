package com.nisovin.magicspells;

import java.util.Map;
import java.util.HashMap;

import com.nisovin.magicspells.util.wrapper.WrappedMaterial;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerAnimationEvent;

import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.mana.ManaChangeReason;

public class CastListener implements Listener {

	MagicSpells plugin;

	private HashMap<String, Long> noCastUntil = new HashMap<>();
	//private HashMap<String,Long> lastCast = new HashMap<String, Long>();

	CastListener(MagicSpells plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();

		// First check if player is interacting with a special block
		boolean noInteract = false;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Material m = event.getClickedBlock().getType();
			if (
					m == WrappedMaterial.getWrapper().getCraftingTable() ||
					m == Material.CHEST ||
					m == Material.TRAPPED_CHEST ||
					m == Material.ENDER_CHEST ||
					m == Material.FURNACE ||
					m == Material.HOPPER ||
					m == Material.LEVER ||
					m == Material.STONE_BUTTON ||
					m == WrappedMaterial.getWrapper().getEnchantingTable()) {
				noInteract = true;
			} else if (event.hasItem() && event.getItem().getType().isBlock()) {
				noInteract = true;
			}
			if (m == WrappedMaterial.getWrapper().getEnchantingTable()) {
				// Force exp bar back to show exp when trying to enchant
				MagicSpells.getExpBarManager().update(player, player.getLevel(), player.getExp());
			}
		}

		if (noInteract) {
			// Special block -- don't do normal interactions
			noCastUntil.put(event.getPlayer().getName(), System.currentTimeMillis() + 150);
			return;
		}

		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			// Left click - cast
			if (!plugin.castOnAnimate) castSpell(event.getPlayer());
			return;
		}

		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && (plugin.cycleSpellsOnOffhandAction || event.getHand() == EquipmentSlot.HAND)) {
			// Right click -- cycle spell and/or process mana pots
			ItemStack inHand = player.getEquipment().getItemInMainHand();
			if ((inHand != null && !BlockUtils.isAir(inHand.getType())) || plugin.allowCastWithFist) {

				// Cycle spell
				Spell spell = null;
				if (!player.isSneaking()) spell = MagicSpells.getSpellbook(player).nextSpell(inHand);
				else spell = MagicSpells.getSpellbook(player).prevSpell(inHand);

				if (spell != null) {
					// Send message
					MagicSpells.sendMessageAndFormat(player, plugin.strSpellChange, "%s", spell.getName());
					// Show spell icon
					if (plugin.spellIconSlot >= 0) {
						showIcon(player, plugin.spellIconSlot, spell.getSpellIcon());
					}
					// Use cool new text thingy
					boolean yay = false;
					if (yay) {
						final ItemStack fake = inHand.clone(); //TODO ensure this is not null
						ItemMeta meta = fake.getItemMeta();
						meta.setDisplayName("Spell: " + spell.getName());
						fake.setItemMeta(meta);
						MagicSpells.scheduleDelayedTask(() -> MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, player.getInventory().getHeldItemSlot(), fake), 0);
					}
				}

				// Check for mana pots
				if (plugin.enableManaBars && MagicSpells.getManaPotions() != null) {
					// Find mana potion TODO: fix this, it's not good
					int restoreAmt = 0;
					for (Map.Entry<ItemStack, Integer> entry : MagicSpells.getManaPotions().entrySet()) {
						if (inHand.isSimilar(entry.getKey())) { //TODO make sure this is not null
							restoreAmt = entry.getValue();
							break;
						}
					}
					if (restoreAmt > 0) {
						// Check cooldown
						if (plugin.manaPotionCooldown > 0) {
							Long c = MagicSpells.getManaPotionCooldowns().get(player);
							if (c != null && c > System.currentTimeMillis()) {
								MagicSpells.sendMessage(plugin.strManaPotionOnCooldown.replace("%c", "" + (int)((c - System.currentTimeMillis())/TimeUtil.MILLISECONDS_PER_SECOND)), player, MagicSpells.NULL_ARGS);
								return;
							}
						}
						// Add mana
						boolean added = MagicSpells.getManaHandler().addMana(player, restoreAmt, ManaChangeReason.POTION);
						if (added) {
							// Set cooldown
							if (plugin.manaPotionCooldown > 0) {
								MagicSpells.getManaPotionCooldowns().put(player, System.currentTimeMillis() + plugin.manaPotionCooldown * TimeUtil.MILLISECONDS_PER_SECOND);
							}
							// Remove item
							//TODO make sure this is not null
							if (inHand.getAmount() == 1) inHand = null;
							else inHand.setAmount(inHand.getAmount() - 1);
							player.getEquipment().setItemInMainHand(inHand);
							player.updateInventory();
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onItemHeldChange(final PlayerItemHeldEvent event) {
		if (plugin.spellIconSlot >= 0 && plugin.spellIconSlot <= 8) {
			Player player = event.getPlayer();
			if (event.getNewSlot() == plugin.spellIconSlot) {
				showIcon(player, plugin.spellIconSlot, null);
				return;
			}
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			Spell spell = spellbook.getActiveSpell(player.getInventory().getItem(event.getNewSlot()));
			if (spell != null) showIcon(player, plugin.spellIconSlot, spell.getSpellIcon());
			else showIcon(player, plugin.spellIconSlot, null);
		}
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		if (plugin.castOnAnimate) castSpell(event.getPlayer());
	}

	private void castSpell(Player player) {
		ItemStack inHand = player.getEquipment().getItemInMainHand();
		if (!plugin.allowCastWithFist && (inHand == null || BlockUtils.isAir(inHand.getType()))) return;

		Spell spell = MagicSpells.getSpellbook(player).getActiveSpell(inHand);
		if (spell == null || !spell.canCastWithItem()) return;
		// First check global cooldown
		if (plugin.globalCooldown > 0 && !spell.ignoreGlobalCooldown) {
			if (noCastUntil.containsKey(player.getName()) && noCastUntil.get(player.getName()) > System.currentTimeMillis()) return;
			noCastUntil.put(player.getName(), System.currentTimeMillis() + plugin.globalCooldown);
		}
		// Cast spell
		spell.cast(player);
	}

	private void showIcon(Player player, int slot, ItemStack icon) {
		if (icon == null) icon = player.getInventory().getItem(plugin.spellIconSlot);
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, icon);
	}

}
