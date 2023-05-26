package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class StunSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private Map<UUID, StunnedInfo> stunnedLivingEntities;

	private int taskId = -1;
	private int interval;
	private int duration;

	private Listener listener;
	
	public StunSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		interval = getConfigInt("interval", 5);
		duration = (int) ((getConfigInt("duration", 200) / 20) * TimeUtil.MILLISECONDS_PER_SECOND);

		listener = new StunListener();
		stunnedLivingEntities = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		registerEvents(listener);
		taskId = MagicSpells.scheduleRepeatingTask(new StunMonitor(), interval, interval);
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
			if (targetInfo == null) return noTarget(caster);
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			
			stunLivingEntity(caster, target, Math.round(duration * power.intValue()));
			sendMessages(caster, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		stunLivingEntity(caster, target, Math.round(duration * power.intValue()));
		return true;
	}
	
	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		if (!validTargetList.canTarget(target)) return false;
		stunLivingEntity(null, target, Math.round(duration * power.intValue()));
		return true;
	}
	
	private void stunLivingEntity(LivingEntity caster, LivingEntity target, int duration) {
		StunnedInfo info = new StunnedInfo(caster, target, System.currentTimeMillis() + duration, target.getLocation());
		stunnedLivingEntities.put(target.getUniqueId(), info);
		
		if (caster != null) playSpellEffects(caster, target);
		else playSpellEffects(EffectPosition.TARGET, target);
		
		playSpellEffectsBuff(target, entity -> {
			if (!(entity instanceof LivingEntity)) return false;
			return isStunned((LivingEntity) entity);
		});
		
	}

	public boolean isStunned(LivingEntity entity) {
		return stunnedLivingEntities.containsKey(entity.getUniqueId());
	}
	
	public void removeStun(LivingEntity entity) {
		stunnedLivingEntities.remove(entity.getUniqueId());
	}
	
	private static class StunnedInfo {
		
		private Long until;
		private LivingEntity caster;
		private LivingEntity target;
		private Location targetLocation;
		
		private StunnedInfo(LivingEntity caster, LivingEntity target, Long until, Location targetLocation) {
			this.caster = caster;
			this.target = target;
			this.until = until;
			this.targetLocation = targetLocation;
		}
		
	}
	
	private class StunListener implements Listener {
		
		@EventHandler
		private void onMove(PlayerMoveEvent e) {
			Player pl = e.getPlayer();
			if (!isStunned(pl)) return;
			StunnedInfo info = stunnedLivingEntities.get(pl.getUniqueId());
			if (info == null) return;
			
			if (info.until > System.currentTimeMillis()) {
				e.setTo(info.targetLocation);
				return;
			}
			
			removeStun(pl);
		}
		
		@EventHandler
		private void onInteract(PlayerInteractEvent e) {
			if (!isStunned(e.getPlayer())) return;
			e.setCancelled(true);
		}
		
		@EventHandler
		private void onQuit(PlayerQuitEvent e) {
			Player pl = e.getPlayer();
			if (!isStunned(pl)) return;
			removeStun(pl);
		}
		
		@EventHandler
		private void onDeath(PlayerDeathEvent e) {
			Player pl = e.getEntity();
			if (!isStunned(pl)) return;
			removeStun(pl);
		}
		
	}
	
	private class StunMonitor implements Runnable {

		private Set<LivingEntity> toRemove = new HashSet<>();

		@Override
		public void run() {
			for (UUID id : stunnedLivingEntities.keySet()) {
				StunnedInfo info = stunnedLivingEntities.get(id);
				LivingEntity entity = info.target;
				Long until = info.until;
				if (entity instanceof Player) continue;
				
				if (entity.isValid() && until > System.currentTimeMillis()) {
					entity.teleport(info.targetLocation);
					continue;
				}

				toRemove.add(entity);
			}

			for (LivingEntity entity : toRemove) {
				removeStun(entity);
			}

			toRemove.clear();
		}
		
	}
	
}
