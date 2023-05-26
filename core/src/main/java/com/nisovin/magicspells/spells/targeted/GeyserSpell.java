package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.List;
import java.util.ArrayList;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class GeyserSpell extends TargetedSpell implements TargetedEntitySpell {

	private Material blockType;
	private String blockTypeName;

	private double damage;
	private double velocity;

	private int geyserHeight;
	private int animationSpeed;

	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean avoidDamageModification;

	public GeyserSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		blockTypeName = getConfigString("geyser-type", "water").toUpperCase();
		blockType = Material.getMaterial(blockTypeName);
		if (blockType == null || !blockType.isBlock()) {
			MagicSpells.error("GeyserSpell '" + internalName + "' has an invalid geyser-type defined!");
			blockType = null;
		}

		damage = getConfigFloat("damage", 0);
		velocity = getConfigInt("velocity", 10) / 10.0F;

		geyserHeight = getConfigInt("geyser-height", 4);
		animationSpeed = getConfigInt("animation-speed", 2);

		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			boolean ok = geyser(livingEntity, target.getTarget(), target.getPower());
			if (!ok) return noTarget(livingEntity);

			playSpellEffects(livingEntity, target.getTarget());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean geyser(LivingEntity caster, LivingEntity target, Power power) {
		double dam = damage * power.doubleValue();
		
		if (caster != null && checkPlugins && damage > 0) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, dam);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			if (!avoidDamageModification) dam = event.getDamage();
		}
		
		if (dam > 0) {
			if (ignoreArmor) {
				double health = target.getHealth() - dam;
				if (health < 0) health = 0;
				target.setHealth(health);
				target.playEffect(EntityEffect.HURT);
			} else {
				if (caster != null) target.damage(dam, caster);
				else target.damage(dam);
			}
		}
		
		if (velocity > 0) target.setVelocity(new Vector(0, velocity * power.doubleValue(), 0));
		
		if (geyserHeight > 0) {
			List<Entity> allNearby = target.getNearbyEntities(50, 50, 50);
			allNearby.add(target);
			List<Player> playersNearby = new ArrayList<>();
			for (Entity e : allNearby) {
				if (!(e instanceof Player)) continue;
				playersNearby.add((Player) e);
			}
			new GeyserAnimation(target.getLocation(), playersNearby);
		}
		
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		geyser(caster, target, power);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		if (!validTargetList.canTarget(target)) return false;
		
		geyser(null, target, power);
		playSpellEffects(EffectPosition.TARGET, target);
		return true;
	}
	
	private class GeyserAnimation extends SpellAnimation {

		private Location start;
		private List<Player> nearby;
		
		private GeyserAnimation(Location start, List<Player> nearby) {
			super(0, animationSpeed, true);

			this.start = start;
			this.nearby = nearby;
		}

		@Override
		protected void onTick(int tick) {
			if (tick > geyserHeight << 1) {
				stop();
				return;
			}

			if (tick < geyserHeight) {
				Block block = start.clone().add(0, tick, 0).getBlock();
				if (!BlockUtils.isAir(block.getType())) return;
				for (Player p : nearby)
					sendBlockChange(p,block.getLocation(),blockType);
				return;
			}

			int n = geyserHeight - (tick - geyserHeight) - 1;
			Block block = start.clone().add(0, n, 0).getBlock();
			for (Player p : nearby) {
				sendBlockChange(p,block.getLocation(),block.getType());
			}
		}
		
	}

	private static void sendBlockChange(Player player, Location location, Material material) {
		ProtocolManager manager = ProtocolLibrary.getProtocolManager();
		PacketContainer container = manager.createPacket(PacketType.Play.Server.BLOCK_CHANGE);
		container.getBlockData().write(0, WrappedBlockData.createData(material));
		container.getBlockPositionModifier().write(0,new BlockPosition(location.getBlockX(),location.getBlockY(),location.getBlockZ()));
		try {
			manager.sendServerPacket(player,container);
		} catch (Exception ignored) {}
	}
}
