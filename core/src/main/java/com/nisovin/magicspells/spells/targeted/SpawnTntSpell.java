package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class SpawnTntSpell extends TargetedSpell implements TargetedLocationSpell {

	private Map<Integer, TntInfo> tnts;

	private int fuse;

	private float velocity;
	private float upVelocity;

	private boolean cancelGravity;
	private boolean cancelExplosion;
	private boolean preventBlockDamage;

	private String spellToCastName;
	private Subspell spellToCast;
	
	public SpawnTntSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		fuse = getConfigInt("fuse", TimeUtil.TICKS_PER_SECOND);

		velocity = getConfigFloat("velocity", 0F);
		upVelocity = getConfigFloat("up-velocity", velocity);

		cancelGravity = getConfigBoolean("cancel-gravity", false);
		cancelExplosion = getConfigBoolean("cancel-explosion", false);
		preventBlockDamage = getConfigBoolean("prevent-block-damage", false);

		spellToCastName = getConfigString("spell", "");

		tnts = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();

		spellToCast = new Subspell(spellToCastName);
		if (!spellToCast.process() || !spellToCast.isTargetedLocationSpell()) {
			if (!spellToCastName.isEmpty()) MagicSpells.error("SpawnTntSpell '" + internalName + "' has an invalid spell defined!");
			spellToCast = null;
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> blocks = getLastTwoTargetedBlocks(caster, power);
			if (blocks.size() == 2 && !blocks.get(0).getType().isSolid() && blocks.get(0).getType().isSolid()) {
				Location loc = blocks.get(0).getLocation().add(0.5, 0.5, 0.5);
				loc.setDirection(caster.getLocation().getDirection());
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		spawnTnt(caster, power, target.clone().add(0.5, 0.5, 0.5));
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		spawnTnt(null, power, target.clone().add(0.5, 0.5, 0.5));
		return true;
	}

	private void spawnTnt(LivingEntity caster, float power, Location loc) {
		TNTPrimed tnt = loc.getWorld().spawn(loc, TNTPrimed.class);

		if (cancelGravity) tnt.setGravity(false);

		playSpellEffects(EffectPosition.PROJECTILE, tnt);
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, caster.getLocation(), tnt.getLocation(), caster, tnt);
		tnt.setFuseTicks(fuse);

		if (velocity > 0) tnt.setVelocity(loc.getDirection().normalize().setY(0).multiply(velocity).setY(upVelocity));
		else if (upVelocity > 0) tnt.setVelocity(new Vector(0, upVelocity, 0));

		tnts.put(tnt.getEntityId(), new TntInfo(caster, power));
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		TntInfo info = tnts.remove(event.getEntity().getEntityId());
		if (info == null) return;

		if (cancelExplosion) {
			event.setCancelled(true);
			event.getEntity().remove();
		}

		if (preventBlockDamage) {
			event.blockList().clear();
			event.setYield(0F);
		}

		for (Block b: event.blockList()) playSpellEffects(EffectPosition.BLOCK_DESTRUCTION, b.getLocation());

		if (spellToCast == null) return;
		if (info.caster == null) return;
		if (!info.caster.isValid()) return;
		if (info.caster.isDead()) return;

		spellToCast.castAtLocation(info.caster, event.getEntity().getLocation(), info.power);
	}
	
	private static class TntInfo {
		
		private LivingEntity caster;
		private float power;
		
		private TntInfo(LivingEntity caster, float power) {
			this.caster = caster;
			this.power = power;
		}
		
	}

}
