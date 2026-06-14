package SS.action.monster;

import java.util.ArrayList;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ExhaustCardEffect;

import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.InscribeCardPower;

public class ExitEvokeSoulEnvAction extends AbstractGameAction {
    @Override
    public void update() {
        ArrayList<SoulAlly> aliveSouls = new ArrayList<>();
        for (AbstractMonster m : AllyManager.allies.monsters) {
            if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                aliveSouls.add((SoulAlly) m);
            }
        }

        if (aliveSouls.isEmpty()) {
            this.isDone = true;
            return;
        }

        // 1. 保留第 0 个魂火作为“火种”
        SoulAlly primarySoul = aliveSouls.get(0);

        // 将第一个魂火复原大小和位置
        primarySoul.modelScale = 1.0f;
        primarySoul.hb.resize(150.0F * Settings.scale, 250.0F * Settings.scale);
        primarySoul.drawX = AbstractDungeon.player.drawX + 300.0F * Settings.scale; // 回到默认位置
        primarySoul.drawY = AbstractDungeon.player.drawY + 250.0F * Settings.scale;
        primarySoul.hb.move(primarySoul.drawX,
                primarySoul.drawY + primarySoul.hb.height / 2.0F + primarySoul.healthBarOffsetY * Settings.scale);
        primarySoul.syncSoulFire(); // 刷新

        primarySoul.revertFromGenesisDeck();

        primarySoul.syncSoulFire();

        // 2. 吞噬融合其余 4 个魂火，并返还它们存着的牌
        for (int i = 1; i < aliveSouls.size(); i++) {
            SoulAlly victim = aliveSouls.get(i);

            // 返还卡牌逻辑
            if (victim.hasPower("Double:InscribeCardPower")) {
                InscribeCardPower power = (InscribeCardPower) victim.getPower("Double:InscribeCardPower");
                if (power.card != null) {
                    AbstractCard card = power.card;
                    card.retain = false; // 取消保留

                    // 特效飞入玩家牌组
                    if (AbstractDungeon.player.hand.size() < basemod.BaseMod.MAX_HAND_SIZE) {
                        AbstractDungeon.effectList
                                .add(new ShowCardAndAddToHandEffect(card, victim.hb.cX, victim.hb.cY));
                    } else {
                        AbstractDungeon.effectList
                                .add(new ShowCardAndAddToDiscardEffect(card, victim.hb.cX, victim.hb.cY));
                    }
                    power.card = null;
                }
            }

            // 燃尽退场，并从友军管理器静默移除
            AllyManager.allies.monsters.remove(victim);
        }

        this.isDone = true;
    }
}