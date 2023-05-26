package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.io.FileReader;
import java.io.BufferedReader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.IDisguiseManager;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class DisguiseSpell extends TargetedSpell implements TargetedEntitySpell {

	private static IDisguiseManager manager;

	private DisguiseSpell thisSpell;

	private Map<UUID, Disguise> disguised = new HashMap<>();

	private EntityData entityData;

	private int duration;

	private String uuid = "";
	private String skin = "";
	private String skinSig = "";
	private String strFade;
	private String nameplateText;

	private boolean toggle;
	private boolean ridingBoat;
	private boolean disguiseSelf;
	private boolean friendlyMobs;
	private boolean showPlayerName;
	private boolean preventPickups;
	private boolean undisguiseOnCast;
	private boolean undisguiseOnDeath;
	private boolean undisguiseOnLogout;
	private boolean alwaysShowNameplate;
	private boolean undisguiseOnGiveDamage;
	private boolean undisguiseOnTakeDamage;

	public DisguiseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		thisSpell = this;

		if (manager == null) {
			try {
				manager = MagicSpells.getVolatileCodeHandler().getDisguiseManager(config);
			} catch (Exception e) {
				manager = null;
			}

			if (manager == null) {
				MagicSpells.error("DisguiseManager could not be created!");
				return;
			}
		}
		manager.registerSpell(this);

		String type = getConfigString("entity-type", "zombie");
		entityData = new EntityData(type);

		duration = getConfigInt("duration", 0);

		uuid = getConfigString("uuid", "");
		strFade = getConfigString("str-fade", "");
		nameplateText = ChatColor.translateAlternateColorCodes('&', getConfigString("nameplate-text", ""));
		if (configKeyExists("skin")) {
			String skinName = getConfigString("skin", "skin");
			File folder = new File(MagicSpells.getInstance().getDataFolder(), "disguiseskins");
			if (folder.exists()) {
				try {
					File file = new File(folder, skinName + ".skin.txt");
					if (file.exists()) {
						BufferedReader reader = new BufferedReader(new FileReader(file));
						skin = reader.readLine();
						reader.close();
					}
					file = new File(folder, skinName + ".sig.txt");
					if (file.exists()) {
						BufferedReader reader = new BufferedReader(new FileReader(file));
						skinSig = reader.readLine();
						reader.close();
					}
				} catch (Exception e) {
					MagicSpells.handleException(e);
				}
			}
		}

		toggle = getConfigBoolean("toggle", false);
		ridingBoat = getConfigBoolean("riding-boat", false);
		disguiseSelf = getConfigBoolean("disguise-self", false);
		friendlyMobs = getConfigBoolean("friendly-mobs", true);
		showPlayerName = getConfigBoolean("show-player-name", false);
		preventPickups = getConfigBoolean("prevent-pickups", true);
		undisguiseOnCast = getConfigBoolean("undisguise-on-cast", false);
		undisguiseOnDeath = getConfigBoolean("undisguise-on-death", true);
		undisguiseOnLogout = getConfigBoolean("undisguise-on-logout", false);
		alwaysShowNameplate = getConfigBoolean("always-show-nameplate", true);
		undisguiseOnGiveDamage = getConfigBoolean("undisguise-on-give-damage", false);
		undisguiseOnTakeDamage = getConfigBoolean("undisguise-on-take-damage", false);

		if (entityData.getType() == null) MagicSpells.error("Invalid entity-type specified for disguise spell '" + spellName + '\'');
	}

	@Override
	public void initialize() {
		if (manager == null) return;
		super.initialize();
		if (undisguiseOnCast) registerEvents(new CastListener());
		if (undisguiseOnGiveDamage || undisguiseOnTakeDamage) registerEvents(new DamageListener());
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (manager == null || !(livingEntity instanceof Player)) return PostCastAction.ALREADY_HANDLED;
		if (state == SpellCastState.NORMAL) {
			Player player = (Player) livingEntity;
			Disguise oldDisguise = disguised.remove(player.getUniqueId());
			manager.removeDisguise(player);
			if (oldDisguise != null && toggle) {
				sendMessage(strFade, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			TargetInfo<Player> target = getTargetPlayer(player, power);
			if (target == null) return noTarget(player);
			disguise(target.getTarget());
			sendMessages(player, target.getTarget());
			playSpellEffects(EffectPosition.CASTER, player);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity player, LivingEntity target, Power power) {
		if (!(target instanceof Player)) return false;
		disguise((Player) target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		if (!(target instanceof Player)) return false;
		disguise((Player) target);
		return true;
	}

	private void disguise(Player player) {
		String nameplate = nameplateText;
		if (showPlayerName) nameplate = player.getDisplayName();
		PlayerDisguiseData playerDisguiseData = new PlayerDisguiseData((uuid.isEmpty() ? UUID.randomUUID().toString() : uuid), skin, skinSig);
		Disguise disguise = new Disguise(player, entityData.getType(), nameplate, playerDisguiseData, alwaysShowNameplate, disguiseSelf, ridingBoat, entityData.getFlag(), entityData.getVar1(), entityData.getVar2(), entityData.getVar3(), duration, this);
		manager.addDisguise(player, disguise);
		disguised.put(player.getUniqueId(), disguise);
		playSpellEffects(EffectPosition.TARGET, player);
	}

	public void undisguise(Player player) {
		Disguise disguise = disguised.remove(player.getUniqueId());
		if (disguise == null) return;

		disguise.cancelDuration();
		sendMessage(strFade, player, MagicSpells.NULL_ARGS);
		playSpellEffects(EffectPosition.DISABLED, player);
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (!preventPickups) return;
		if (!disguised.containsKey(event.getEntity().getUniqueId())) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (!undisguiseOnDeath) return;
		if (!disguised.containsKey(event.getEntity().getUniqueId())) return;
		manager.removeDisguise(event.getEntity(), entityData.getType() == EntityType.PLAYER);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if (!undisguiseOnLogout) return;
		if (!disguised.containsKey(event.getPlayer().getUniqueId())) return;
		manager.removeDisguise(event.getPlayer(), entityData.getType() == EntityType.PLAYER);
	}

	@EventHandler
	public void onTarget(EntityTargetEvent event) {
		if (!friendlyMobs) return;
		if (event.getTarget() == null) return;
		if (!(event.getTarget() instanceof Player)) return;
		if (!disguised.containsKey(event.getTarget().getUniqueId())) return;
		event.setCancelled(true);
	}

	class CastListener implements Listener {

		@EventHandler
		void onSpellCast(SpellCastedEvent event) {
			if (event.getSpell() == thisSpell) return;
			if (!(event.getCaster() instanceof Player)) return;
			if (!disguised.containsKey(event.getCaster().getUniqueId())) return;
			manager.removeDisguise((Player) event.getCaster());
		}

	}

	class DamageListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		void onDamage(EntityDamageEvent event) {
			if (undisguiseOnTakeDamage && event.getEntity() instanceof Player && disguised.containsKey(event.getEntity().getUniqueId())) {
				manager.removeDisguise((Player)event.getEntity());
			}
			if (undisguiseOnGiveDamage && event instanceof EntityDamageByEntityEvent) {
				Entity e = ((EntityDamageByEntityEvent)event).getDamager();
				if (e instanceof Player) {
					if (disguised.containsKey(e.getUniqueId())) manager.removeDisguise((Player)e);
				} else if (e instanceof Projectile && ((Projectile)e).getShooter() instanceof Player) {
					Player shooter = (Player)((Projectile)e).getShooter();
					if (disguised.containsKey(shooter.getUniqueId())) manager.removeDisguise(shooter);
				}
			}
		}

	}

	public static IDisguiseManager getDisguiseManager() {
		return manager;
	}

	@Override
	public void turnOff() {
		if (manager != null) {
			for (UUID id : disguised.keySet()) {
				Player player = Bukkit.getPlayer(id);
				if (player == null) continue;
				manager.removeDisguise(player, false);
			}
			manager.unregisterSpell(this);
			if (manager.registeredSpellsCount() == 0) {
				manager.destroy();
				manager = null;
			}
		}
	}

	public class Disguise {

		Player player;
		private EntityType entityType;
		private String nameplateText;
		private PlayerDisguiseData playerDisguiseData;
		private boolean alwaysShowNameplate;
		private boolean disguiseSelf;
		private boolean ridingBoat;
		private boolean flag;
		private int var1;
		private int var2;
		private int var3;
		private DisguiseSpell spell;

		private int taskId;

		public Disguise(Player player, EntityType entityType, String nameplateText, PlayerDisguiseData playerDisguiseData, boolean alwaysShowNameplate, boolean disguiseSelf, boolean ridingBoat, boolean flag, int var1, int var2, int var3, int duration, DisguiseSpell spell) {
			this.player = player;
			this.entityType = entityType;
			this.nameplateText = nameplateText;
			this.playerDisguiseData = playerDisguiseData;
			this.alwaysShowNameplate = alwaysShowNameplate;
			this.disguiseSelf = disguiseSelf;
			this.ridingBoat = ridingBoat;
			this.flag = flag;
			this.var1 = var1;
			this.var2 = var2;
			this.var3 = var3;
			if (duration > 0) startDuration(duration);
			this.spell = spell;
		}

		public Player getPlayer() {
			return player;
		}

		public EntityType getEntityType() {
			return entityType;
		}

		public String getNameplateText() {
			return nameplateText;
		}

		public PlayerDisguiseData getPlayerDisguiseData() {
			return playerDisguiseData;
		}

		public boolean alwaysShowNameplate() {
			return alwaysShowNameplate;
		}

		public boolean disguiseSelf() {
			return disguiseSelf;
		}

		public boolean isRidingBoat() {
			return ridingBoat;
		}

		public boolean getFlag() {
			return flag;
		}

		public int getVar1() {
			return var1;
		}

		public int getVar2() {
			return var2;
		}

		public int getVar3() {
			return var3;
		}

		private void startDuration(int duration) {
			taskId = MagicSpells.scheduleDelayedTask(() -> DisguiseSpell.manager.removeDisguise(player), duration);
		}

		public void cancelDuration() {
			if (taskId > 0) {
				MagicSpells.cancelTask(taskId);
				taskId = 0;
			}
		}

		public DisguiseSpell getSpell() {
			return spell;
		}

	}

	public class PlayerDisguiseData {

		public String uuid;
		public String skin;
		public String sig;

		public PlayerDisguiseData(String uuid, String skin, String sig) {
			this.uuid = uuid;
			this.skin = skin;
			this.sig = sig;
		}

		@Override
		public PlayerDisguiseData clone() {
			return new PlayerDisguiseData(uuid, skin, sig);
		}

	}

}
