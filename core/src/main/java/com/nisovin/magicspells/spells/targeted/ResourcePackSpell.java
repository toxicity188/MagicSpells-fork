package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;

public class ResourcePackSpell extends TargetedSpell {

	private static final int HASH_LENGTH = 20;

	private String url;
	private byte[] hash = null;
	
	public ResourcePackSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		url = getConfigString("url", null);
		String hashString = getConfigString("hash", null);

		if (hashString != null) {
			hash = hexStringToByteArray(hashString);
			if (hash.length != HASH_LENGTH) {
				MagicSpells.error("Incorrect length for resource pack hash: " + hash.length);
				MagicSpells.error("Avoiding use of the hash to avoid further problems.");
				hash = null;
			}
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			TargetInfo<Player> target = getTargetedPlayer(player, power);
			Player targetPlayer = target.getTarget();
			if (targetPlayer == null) return noTarget(player);

			sendResourcePack(player);
			return PostCastAction.HANDLE_NORMALLY;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void sendResourcePack(Player player) {
		if (hash == null) player.setResourcePack(url);
		else player.setResourcePack(url, hash);
	}
	
	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

}
