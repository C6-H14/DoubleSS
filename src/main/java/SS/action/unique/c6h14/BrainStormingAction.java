package SS.action.unique.c6h14;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;

import basemod.BaseMod;

public class BrainStormingAction extends AbstractGameAction {
    private boolean retrieveCard = false;
    private AbstractCard baseCard; // 传入自身，以便复制

    public BrainStormingAction(AbstractCard baseCard) {
        this.actionType = ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.baseCard = baseCard;
    }

    @Override
    public void update() {
        // 第一阶段：打开发现界面
        if (this.duration == Settings.ACTION_DUR_FAST) {
            // 1. 生成 3 张待选牌
            ArrayList<AbstractCard> generatedCards = new ArrayList<>();

            // 必定包含一张自身的复制品 (保留原有的升级状态等)
            generatedCards.add(this.baseCard.makeStatEquivalentCopy());

            // 补充 2 张随机牌
            while (generatedCards.size() < 3) {
                AbstractCard randomCard = AbstractDungeon.returnTrulyRandomCardInCombat().makeCopy();

                // 去重检测：防止出现两张一模一样的随机牌
                boolean isDuplicate = false;
                for (AbstractCard c : generatedCards) {
                    if (c.cardID.equals(randomCard.cardID)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (!isDuplicate) {
                    generatedCards.add(randomCard);
                }
            }

            // 2. 打乱顺序，防止复制品永远在最左边
            Collections.shuffle(generatedCards, new Random(AbstractDungeon.cardRandomRng.randomLong()));

            // 3. 打开自定义发现界面 (参数3: true表示卡牌不要花费金币)
            AbstractDungeon.cardRewardScreen.customCombatOpen(generatedCards, CardRewardScreen.TEXT[1], true);

            this.tickDuration();
            return;
        }

        // 第二阶段：玩家做出了选择，处理选中的牌
        if (!this.retrieveCard) {
            if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                // 获取玩家选中的卡牌
                AbstractCard discoveredCard = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                discoveredCard.current_x = -1000.0F * Settings.scale; // 从屏幕外飞进来

                // 判断手牌是否已满
                if (AbstractDungeon.player.hand.size() < BaseMod.MAX_HAND_SIZE) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(discoveredCard, Settings.WIDTH / 2.0F,
                            Settings.HEIGHT / 2.0F));
                } else {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(discoveredCard,
                            Settings.WIDTH / 2.0F, Settings.HEIGHT / 2.0F));
                }

                // 清理标志位
                AbstractDungeon.cardRewardScreen.discoveryCard = null;
            }
            this.retrieveCard = true;
        }

        this.tickDuration();
    }
}