package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class LeapSpell extends InstantSpell {

	private final int degreeAmount;
	private final float rotation;
	private final double forwardVelocity;
	private final double upwardVelocity;
	private final boolean cancelDamage;
	private final boolean clientOnly;
	private final boolean playerMovement;
	private Subspell landSpell;
	private final String landSpellName;

	private final Set<LivingEntity> jumping;

	public LeapSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		this.jumping = new HashSet<>();

		this.rotation = getConfigFloat("rotation", 0F);
		this.forwardVelocity = getConfigInt("forward-velocity", 40) / 10D;
		this.upwardVelocity = getConfigInt("upward-velocity", 15) / 10D;
		this.cancelDamage = getConfigBoolean("cancel-damage", false);
		this.clientOnly = getConfigBoolean("client-only", false);
		this.landSpellName = getConfigString("land-spell", "");
		int d = getConfigInt("degree-amount", 8);
		this.degreeAmount = 360 / ((d > 0) ? d : 8);
		this.playerMovement = getConfigBoolean("player-movement", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		landSpell = new Subspell(landSpellName);
		if (!landSpell.process()) {
			if (!landSpellName.isEmpty()) MagicSpells.error("Leap Spell '" + internalName + "' has an invalid land-spell defined!");
			landSpell = null;
		}
	}



	public boolean isJumping(LivingEntity pl) {
		return jumping.contains(pl);
	}

	@Override
	public PostCastAction castSpell(LivingEntity player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (playerMovement) {
				double x = player.getLocation().getX();
				double z = player.getLocation().getZ();
				double yaw = player.getLocation().getYaw();
				MagicSpells.scheduleDelayedTask(() -> {
					double degree = Math.atan2(player.getLocation().getZ() - z, player.getLocation().getX() - x);
					if (degree != 0) {
						degree = degree *180/Math.PI - 90;
						if (degree < 0) degree += 360;
						degree -= yaw;
					}
					leap(player,power,rotation + (float) Math.round(degree/degreeAmount) * degreeAmount);
				}, 1);

			} else leap(player,power,rotation);
		}

		return PostCastAction.HANDLE_NORMALLY;
	}
	private void leap(LivingEntity player, float power, float rot) {
		Vector v = player.getLocation().getDirection();
		v.setY(0).normalize().multiply(forwardVelocity * power).setY(upwardVelocity * power);
		if (rot != 0) Util.rotateVector(v, rot);
		if (clientOnly && player instanceof Player) {
			MagicSpells.getVolatileCodeHandler().setClientVelocity((Player) player, v);
		} else {
			player.setVelocity(v);
		}
		jumping.add(player);
		playSpellEffects(EffectPosition.CASTER, player);
	}


	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player)) return;
		Player pl = (Player)e.getEntity();
		if (jumping.isEmpty()) return;
		if (!jumping.remove(pl)) return;
		if (landSpell != null) landSpell.cast(pl, 1);
		playSpellEffects(EffectPosition.TARGET, pl.getLocation());
		if (cancelDamage) e.setCancelled(true);
	}

}
