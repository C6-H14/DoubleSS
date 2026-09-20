package SS.action.dice;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

import SS.Dice.EmptyDiceSlot;

public class EvokeAllDiceAction extends AbstractGameAction {
    public EvokeAllDiceAction(int amount) {
        this.amount = amount;
    }

    public void update() {
        // 战斗统计：本 action 由 Seething/Blitzkrieg 这类「额外激发场上已有骰子」的牌产生，
        // 激发瞬间卡牌栈顶仍是该牌的出牌帧 → 把它设为归属覆盖牌，使本次激发的伤害
        // 记在激发牌头上（而非当初产骰的牌）。仅统计用途，不改战斗行为。
        boolean stats = SS.stats.CardStats.enabled;
        if (stats) {
            SS.stats.CardStats.setEvokeAttribution(SS.stats.CardStats.stackTopCard());
        }
        try {
            for (int i = 0; i < AbstractDungeon.player.orbs.size(); i++) {
                if (((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyDiceSlot)
                        || ((AbstractOrb) AbstractDungeon.player.orbs.get(i) instanceof EmptyOrbSlot)) {
                    continue;
                }
                for (int j = 1; j <= this.amount; ++j) {
                    ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).triggerEvokeAnimation();
                    ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).onEvoke();
                }
                AbstractDungeon.player.orbs.set(i, new EmptyDiceSlot());
                ((AbstractOrb) AbstractDungeon.player.orbs.get(i)).setSlot(i, AbstractDungeon.player.maxOrbs);
            }
        } finally {
            if (stats) {
                SS.stats.CardStats.setEvokeAttribution(null);
            }
        }
        this.isDone = true;
    }
}