package com.nisovin.magicspells.volatilecode;

import com.nisovin.magicspells.util.ParticleUtil;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Fireball;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.SmallFireball;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.DisguiseManager;

public class VolatileCodeDisabled implements VolatileCodeHandle {

	public VolatileCodeDisabled() {
		
	}

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
		// Need the volatile code for this
	}

	@Override
	public void creaturePathToLoc(Creature creature, Location loc, float speed) {
		// Need the volatile code for this
	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
		// Need the volatile code for this
	}


	@Override
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire) {
		return false;
	}

	@Override
	public boolean createExplosionByPlayer(Player player, Location location, float size, boolean fire, boolean breakBlocks) {
		return location.getWorld().createExplosion(location, size, fire);
	}

	@Override
	public void setExperienceBar(Player player, int level, float percent) {
		// Need the volatile code for this
	}

	@Override
	public void setTarget(LivingEntity entity, LivingEntity target) {
		if (entity instanceof Creature) ((Creature) entity).setTarget(target);
	}

	@Override
	public void playSound(Location location, String sound, float volume, float pitch) {
		for (Player player : location.getWorld().getPlayers()) {
			player.playSound(location, sound, volume, pitch);
		}
	}

	@Override
	public void playSound(Player player, String sound, float volume, float pitch) {
		player.playSound(player.getLocation(), sound, volume, pitch);
	}

	@Override
	public ItemStack addFakeEnchantment(ItemStack item) {
		return item;
	}


	@Override
	public void createFireworksExplosion(Location location, boolean flicker, boolean trail, int type, int[] colors, int[] fadeColors, int flightDuration) {
		FireworkEffect.Type t = FireworkEffect.Type.BALL;
		if (type == 1) t = FireworkEffect.Type.BALL_LARGE;
		else if (type == 2) t = FireworkEffect.Type.STAR;
		else if (type == 3) t = FireworkEffect.Type.CREEPER;
		else if (type == 4) t = FireworkEffect.Type.BURST;

		Color[] c1 = new Color[colors.length];
		for (int i = 0; i < colors.length; i++) {
			c1[i] = Color.fromRGB(colors[i]);
		}
		Color[] c2 = new Color[fadeColors.length];
		for (int i = 0; i < fadeColors.length; i++) {
			c2[i] = Color.fromRGB(fadeColors[i]);
		}
		FireworkEffect effect = FireworkEffect.builder()
				.flicker(flicker)
				.trail(trail)
				.with(t)
				.withColor(c1)
				.withFade(c2)
				.build();
		Firework firework = location.getWorld().spawn(location, Firework.class);
		FireworkMeta meta = firework.getFireworkMeta();
		meta.addEffect(effect);
		meta.setPower(0);
		firework.setFireworkMeta(meta);
		firework.setSilent(true);
		MagicSpells.scheduleDelayedTask(() -> {
			if (firework == null) return;
			if (!firework.isValid()) return;
			if (firework.isDead()) return;
			firework.detonate();
		}, flightDuration);
	}


	@Override
	public void playParticleEffect(Location location, String name, float spreadX, float spreadY, float spreadZ, float speed, int count, int radius, float yOffset) {
		location.getWorld().spawnParticle(ParticleUtil.ParticleEffect.getParticle(name), location.add(0.0, (double) yOffset, 0.0), count, (double) spreadX, (double) spreadY, (double) spreadZ, speed);
	}

	@Override
	public void playDragonDeathEffect(Location location) {
		// Need the volatile code for this
	}

	@Override
	public void setKiller(LivingEntity entity, Player killer) {
		// Need the volatile code for this
	}

	@Override
	public DisguiseManager getDisguiseManager(MagicConfig config) {
		return null;
	}

	@Override
	public ItemStack addAttributes(ItemStack item, String[] names, String[] types, double[] amounts, int[] operations, String[] slots) {
		// Need the volatile code for this
		return item;
	}

	@Override
	public void removeAI(LivingEntity entity) {
		// Need the volatile code for this
	}

	@Override
	public void addEntityAttribute(LivingEntity entity, String attribute, double amount, int operation) {
		Attribute attr = null;
		switch (attribute) {
			case "generic.maxHealth":
				attr = Attribute.GENERIC_MAX_HEALTH;
				break;
			case "generic.followRange":
				attr = Attribute.GENERIC_FOLLOW_RANGE;
				break;
			case "generic.knockbackResistance":
				attr = Attribute.GENERIC_KNOCKBACK_RESISTANCE;
				break;
			case "generic.movementSpeed":
				attr = Attribute.GENERIC_MOVEMENT_SPEED;
				break;
			case "generic.attackDamage":
				attr = Attribute.GENERIC_ATTACK_DAMAGE;
				break;
			case "generic.attackSpeed":
				attr = Attribute.GENERIC_ATTACK_SPEED;
				break;
			case "generic.armor":
				attr = Attribute.GENERIC_ARMOR;
				break;
			case "generic.luck":
				attr = Attribute.GENERIC_LUCK;
				break;
		}

		Operation oper = null;
		if (operation == 0) oper = Operation.ADD_NUMBER;
		else if (operation == 1) oper = Operation.MULTIPLY_SCALAR_1;
		else if (operation == 2) oper = Operation.ADD_SCALAR;

		if (attr == null || oper == null) return;
		entity.getAttribute(attr).addModifier(new AttributeModifier("MagicSpells " + attribute, amount, oper));
	}

	@Override
	public void addAILookAtPlayer(LivingEntity entity, int range) {
		// Need the volatile code for this
	}

	@Override
	public void saveSkinData(Player player, String name) {
		// Need the volatile code for this
	}

	@Override
	public ItemStack setUnbreakable(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		meta.setUnbreakable(true);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public void sendTitleToPlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
	}

	@Override
	public void sendActionBarMessage(Player player, String message) {
		player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new TextComponent(message));
	}

	@Override
	public ItemStack hideTooltipCrap(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
		meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public void setClientVelocity(Player player, Vector velocity) {
		// Need the volatile code for this
	}

	@Override
	public double getAbsorptionHearts(LivingEntity entity) {
		// Need the volatile code for this
		return 0;
	}

	@Override
	public void showItemCooldown(Player player, ItemStack item, int duration) {
		// Need volatile code for this
	}


	@Override
	public void setSkin(Player player, String skin, String signature) {
		// Need volatile code for this
	}

	@Override
	public void setTexture(SkullMeta meta, String texture, String signature, String uuid, String name) {
		// Need volatile code for this
	}

	@Override
	public void turnOff() {

	}
}
