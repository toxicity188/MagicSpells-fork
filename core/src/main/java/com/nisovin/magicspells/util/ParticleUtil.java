package com.nisovin.magicspells.util;

import java.util.Map;
import java.util.HashMap;
import org.bukkit.Particle;

public class ParticleUtil {

	public enum ParticleEffect {

		EXPLOSION_NORMAL( "explosion_normal", "poof"),
		EXPLOSION_LARGE( "explosion_large", "explosion","largeexplode"),
		EXPLOSION_HUGE( "explosion_huge", "explosion_emitter","hugeexplosion"),
		FIREWORKS_SPARK( "fireworks_spark", "firework"),
		WATER_BUBBLE( "water_bubble", "bubble"),
		WATER_SPLASH( "water_splash", "splash"),
		WATER_WAKE( "water_wake", "fishing"),
		SUSPENDED( "suspended", "underwater"),
		SUSPENDED_DEPTH( "suspended_depth"),
		CRIT( "crit"),
		CRIT_MAGIC( "crit_magic", "enchanted_hit","magiccrit"),
		SMOKE_NORMAL( "smoke_normal", "smoke"),
		SMOKE_LARGE( "smoke_large", "large_smoke","largesmoke"),
		SPELL( "spell", "effect"),
		SPELL_INSTANT( "spell_instant", "instant_effect","instantspell"),
		SPELL_MOB( "spell_mob", "entity_effect","mobSpell"),
		SPELL_MOB_AMBIENT( "spell_mob_ambient", "ambient_entity_effect"),
		SPELL_WITCH( "spell_witch", "witch","witchmagic"),
		DRIP_WATER( "drip_water", "dripping_water","dripwater"),
		DRIP_LAVA( "drip_lava", "dripping_lava","driplava"),
		VILLAGER_ANGRY( "villager_angry", "angry_villager","angryvillager"),
		VILLAGER_HAPPY( "villager_happy", "happy_villager","happyvillager"),
		TOWN_AURA( "town_aura", "mycelium","townaura"),
		NOTE( "note"),
		PORTAL( "portal"),
		ENCHANTMENT_TABLE( "enchantment_table", "enchant"),
		FLAME( "flame"),
		LAVA( "lava"),
		CLOUD( "cloud"),
		REDSTONE( "redstone", "dust","reddust"),
		SNOWBALL( "snowball", "item_snowball","snowballpoof"),
		SNOW_SHOVEL( "snow_shovel"),
		SLIME( "slime", "item_slime"),
		HEART( "heart"),
		BARRIER( "barrier"),
		ITEM_CRACK( "item_crack", "item"),
		BLOCK_CRACK( "block_crack", "blockcrack"),
		BLOCK_DUST( "block_dust", "blockdust", "block"),
		WATER_DROP( "water_drop", "rain"),
		MOB_APPEARANCE( "mob_appearance", "elder_guardian"),
		DRAGON_BREATH( "dragon_breath"),
		END_ROD( "end_rod"),
		DAMAGE_INDICATOR( "damage_indicator"),
		SWEEP_ATTACK( "sweep_attack"),
		FALLING_DUST( "falling_dust"),
		TOTEM( "totem", "totem_of_undying"),
		SPIT( "spit"),
		SQUID_INK( "squid_ink"),
		BUBBLE_POP( "bubble_pop"),
		CURRENT_DOWN( "current_down"),
		BUBBLE_COLUMN_UP( "bubble_column_up"),
		NAUTILUS( "nautilus"),
		DOLPHIN( "dolphin");

		private final String[] names;

		ParticleEffect(String... names) {
			this.names = names;
		}

		private static Map<String, Particle> namesToType = new HashMap<>();
		private static boolean initialized = false;

		private static void initialize() {
			if (initialized) return;

			for (ParticleEffect pe : ParticleEffect.values()) {
				try {
					Particle particle = Particle.valueOf(pe.name());
					// handle the names
					namesToType.put(pe.name().toLowerCase(), particle);
					for (String s : pe.names) {
						namesToType.put(s.toLowerCase(), particle);
					}
				} catch (Exception ignored) {}

			}

			initialized = true;
		}

		public static Particle getParticle(String particle) {
			initialize();
			return namesToType.get(particle.toLowerCase());
		}

	}

}
