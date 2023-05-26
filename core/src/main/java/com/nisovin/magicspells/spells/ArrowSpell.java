package com.nisovin.magicspells.spells;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import com.nisovin.magicspells.power.Power;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ArrowSpell extends Spell {

	private static final String METADATA_KEY = "MSArrowSpell";

	private static ArrowSpellHandler handler;
	
	private String bowName;
	private String spellOnHitEntityName;
	private String spellOnHitGroundName;

	private Subspell spellOnHitEntity;
	private Subspell spellOnHitGround;
	
	private boolean useBowForce;

	public ArrowSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		bowName = ChatColor.translateAlternateColorCodes('&', getConfigString("bow-name", ""));
		spellOnHitEntityName = getConfigString("spell-on-hit-entity", "");
		spellOnHitGroundName = getConfigString("spell-on-hit-ground", "");

		useBowForce = getConfigBoolean("use-bow-force", true);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellOnHitEntity = new Subspell(spellOnHitEntityName);
		if (!spellOnHitEntity.process() || !spellOnHitEntity.isTargetedEntitySpell()) {
			if (!spellOnHitEntityName.isEmpty()) MagicSpells.error("ArrowSpell '" + internalName + "' has an invalid spell-on-hit-entity defined!");
			spellOnHitEntity = null;
		}

		spellOnHitGround = new Subspell(spellOnHitGroundName);
		if (!spellOnHitGround.process() || !spellOnHitGround.isTargetedLocationSpell()) {
			if (!spellOnHitGroundName.isEmpty()) MagicSpells.error("ArrowSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
			spellOnHitGround = null;
		}

		if (handler == null) handler = new ArrowSpellHandler();
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
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
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
	
	private class ArrowSpellHandler implements Listener {

		private Map<String, ArrowSpell> spells = new HashMap<>();
		
		ArrowSpellHandler() {
			registerEvents(this);
		}

		private void registerSpell(ArrowSpell spell) {
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
			ArrowSpell spell = spells.get(bowName);
			if (spell == null) return;
			if (!spellbook.hasSpell(spell)) return;
			if (!spellbook.canCast(spell)) return;

			SpellReagents reagents = spell.reagents.clone();
			SpellCastEvent castEvent = new SpellCastEvent(spell, shooter, SpellCastState.NORMAL, new Power(useBowForce ? event.getForce() : 1.0F), null, cooldown, reagents, castTime);
			EventUtil.call(castEvent);
			Entity projectile = event.getProjectile();
			if (!castEvent.isCancelled()) {
				projectile.setMetadata(METADATA_KEY, new FixedMetadataValue(MagicSpells.plugin, new ArrowSpellData(spell, castEvent.getPower(), castEvent.getReagents())));
				spell.playSpellEffects(EffectPosition.PROJECTILE, event.getProjectile());
				spell.playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, shooter.getLocation(), projectile.getLocation(), shooter, projectile);
			} else {
				event.setCancelled(true);
				projectile.remove();
			}
		}

		@EventHandler
		public void onArrowHit(ProjectileHitEvent event) {
			final Projectile arrow = event.getEntity();
			if (arrow.getType() != EntityType.ARROW) return;
			List<MetadataValue> metas = arrow.getMetadata(METADATA_KEY);
			if (metas == null || metas.isEmpty()) return;
			for (MetadataValue meta : metas) {
				final ArrowSpellData data = (ArrowSpellData) meta.value();
				if (data.spell.spellOnHitGround == null) continue;
				MagicSpells.scheduleDelayedTask(() -> {
					Player shooter = (Player) arrow.getShooter();
					if (data.casted) return;
					if (data.spell.onCooldown(shooter)) {
						MagicSpells.sendMessage(formatMessage(strOnCooldown, "%c", Math.round(getCooldown(shooter)) + ""), shooter, null);
						return;
					}
					if (!data.spell.hasReagents(shooter, data.arrowSpellDataReagents)) {
						MagicSpells.sendMessage(strMissingReagents, shooter, null);
						return;
					}

					boolean success = data.spell.spellOnHitGround.castAtLocation(shooter, arrow.getLocation(), data.power);
					if (success) {
						data.spell.setCooldown(shooter, data.spell.cooldown);
						data.spell.removeReagents(shooter, data.arrowSpellDataReagents);
					}
					data.casted = true;
					arrow.removeMetadata(METADATA_KEY, MagicSpells.plugin);
				}, 0);
				break;
			}
			arrow.remove();
		}

		@EventHandler(ignoreCancelled=true)
		public void onArrowHitEntity(EntityDamageByEntityEvent event) {
			if (event.getDamager().getType() != EntityType.ARROW) return;
			if (!(event.getEntity() instanceof LivingEntity)) return;
			Projectile arrow = (Projectile) event.getDamager();
			List<MetadataValue> metas = arrow.getMetadata(METADATA_KEY);
			if (metas == null || metas.isEmpty()) return;
			Player shooter = (Player) arrow.getShooter();
			for (MetadataValue meta : metas) {
				ArrowSpellData data = (ArrowSpellData) meta.value();
				if (data.spell.onCooldown(shooter)) continue;
				if (data.spell.spellOnHitEntity != null) {
					SpellTargetEvent evt = new SpellTargetEvent(data.spell, shooter, (LivingEntity) event.getEntity(), data.power);
					EventUtil.call(evt);
					if (!evt.isCancelled()) {
						data.spell.spellOnHitEntity.castAtEntity(shooter, (LivingEntity) event.getEntity(), evt.getPower());
						data.spell.setCooldown(shooter, data.spell.cooldown);
					}
					data.casted = true;
				}
				break;
			}
			arrow.remove();
			arrow.removeMetadata(METADATA_KEY, MagicSpells.plugin);
		}

		private void turnOff() {
			unregisterEvents(this);
			spells.clear();
		}
		
	}
	
	private static class ArrowSpellData {

		private ArrowSpell spell;
		private Power power;
		private SpellReagents arrowSpellDataReagents;
		private boolean casted = false;

		ArrowSpellData(ArrowSpell spell, Power power, SpellReagents reagents) {
			this.spell = spell;
			this.power = power;
			arrowSpellDataReagents = reagents;
		}
		
	}

}
