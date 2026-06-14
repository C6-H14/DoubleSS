package SS.action.unique.c6h14;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.InscribeCardPower;

public class LocalizationAction extends AbstractGameAction {
    private static final UIStrings uiStrings;
    public static final String[] TEXT;
    // 状态机变量
    private boolean isInitialized = false;
    private int dmgAmount = 0; // 累计要追加的伤害
    private int blockAmount = 0; // 累计要追加的格挡
    private boolean isUpgraded = false; // 是否是升级版

    public LocalizationAction(boolean isUpgraded) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.isUpgraded = isUpgraded;
    }

    @Override
    public void update() {

        // =================================================================
        // 阶段 1：扫描友军，统计数值，并打开手牌选择界面 (第一帧执行)
        // =================================================================
        if (!isInitialized) {

            // 逆序遍历所有友军
            for (int i = AllyManager.allies.monsters.size() - 1; i >= 0; i--) {
                AbstractMonster m = AllyManager.allies.monsters.get(i);

                // 判断是否是活着的魂火
                if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                    SoulAlly ally = (SoulAlly) m;

                    // 【核心修复】：原代码中的 source 应当修改为当前遍历到的 ally
                    if (ally.hasPower("Double:InscribeCardPower")) {
                        InscribeCardPower power = (InscribeCardPower) ally.getPower("Double:InscribeCardPower");
                        if (power.card == null) {
                            continue;
                        }

                        // 1. 累加被铭刻卡牌的基础属性
                        this.dmgAmount += power.card.baseDamage;
                        this.blockAmount += power.card.baseBlock;

                        // 数值限幅，防止溢出
                        this.dmgAmount = Math.min(this.dmgAmount, 99999);
                        this.blockAmount = Math.min(this.blockAmount, 99999);

                        // 2. 打出并消耗这名友军存的牌
                        // 注意：你需要确保你的 InscribeCardPower 确实有 playAndExhaustCard() 这个方法
                        power.playAndExhaustCard();
                    }
                }
            }

            // 【核心修复】：保底机制。如果没有铭刻任何卡，则攻防各 +3
            if (isUpgraded) {
                this.dmgAmount = Math.max(this.dmgAmount, 3);
                this.blockAmount = Math.max(this.blockAmount, 3);
            }

            if (AbstractDungeon.player.hand.isEmpty()) {
                this.isDone = true;
                return;
            }

            // 3. 打开原版选牌界面
            // 参数说明：提示语，选几张，是否可以少选（false代表必须选满），是否可以不选
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false, false);

            this.isInitialized = true;
            this.tickDuration();
            return; // 这一帧到此结束，进入等待阶段
        }

        // =================================================================
        // 阶段 2：等待玩家做出选择，并给选中的卡牌追加数值 (后续帧执行)
        // =================================================================
        // wereCardsRetrieved 是原版标志位，当玩家点完“确定”后，它会变为 false
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {

            // 遍历玩家选中的卡牌（因为上面限制了选1张，所以这里面只有1个元素）
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                // 1. 永久（在本场战斗中）增加该卡牌的基础数值
                c.baseDamage += this.dmgAmount;
                c.baseBlock += this.blockAmount;

                // 2. 标记数值已被修改（让字体变绿，提示玩家）
                if (this.dmgAmount > 0) {
                    c.isDamageModified = true;
                }
                if (this.blockAmount > 0) {
                    c.isBlockModified = true;
                }

                // 3. 刷新卡牌描述文本和加成
                c.applyPowers();
                c.initializeDescription();

                // 4. 将卡牌重新放回玩家手牌
                AbstractDungeon.player.hand.addToTop(c);
            }

            // 5. 【核心清理】：清空选牌器的缓存，并重置标志位，告诉游戏“玩家已拿回卡牌”
            // 这一步如果不做，游戏会卡在选牌界面无法退回战斗！
            AbstractDungeon.handCardSelectScreen.selectedCards.clear();
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;

            this.isDone = true; // 动作彻底完成
        }

        this.tickDuration();
    }

    static {
        uiStrings = CardCrawlGame.languagePack.getUIString("Double:ChooseHandCardAction");
        TEXT = uiStrings.TEXT;
    }
}