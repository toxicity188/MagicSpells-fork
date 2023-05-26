package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.power.Power;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ConfigReaderUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.prompt.ConversationContextUtil;

public class ConversationSpell extends TargetedSpell implements TargetedEntitySpell {

	private ConversationFactory conversationFactory;
	
	public ConversationSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		conversationFactory = ConfigReaderUtil.readConversationFactory(getConfigSection("conversation"));
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, Power power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
			if (targetInfo == null || targetInfo.getTarget() == null) return noTarget(livingEntity);

			conversate(targetInfo.getTarget());
			return PostCastAction.HANDLE_NORMALLY;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, Power power) {
		conversate(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, Power power) {
		return false;
	}

	private void conversate(LivingEntity target) {
		if (target == null || !(target instanceof Player)) return;
		Conversation c = conversationFactory.buildConversation((Player) target);
		ConversationContextUtil.setconversable(c.getContext(), (Player) target);
		c.begin();
	}

}
