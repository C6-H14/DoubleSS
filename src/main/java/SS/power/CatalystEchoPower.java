package SS.power;

import com.megacrit.cardcrawl.cards.green.Catalyst;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.action.common.EchoACardAction;
import SS.helper.ModHelper;

/**
 * 《催逝员》的下回合延迟效果：回合开始时把手牌塞入 N 张【催化剂的回响】。
 *
 * <p>替代原版 {@code NightmarePower}。NightmarePower 塞的是普通 Catalyst
 * （{@code MakeTempCardInHandAction}），而这牌按设计要给的是「回响版」——
 * 即带【虚无 + 消耗 + Echo 标签】的那种，与《震荡的回响》《怨灵回响》同一套机制。
 *
 * <p>时机与原版一致：都在 {@code atStartOfTurn}。回响张数存在 {@code amount} 里。
 * 触发后自毁（与原版 NightmarePower 一样是一次性的）。
 */
public class CatalystEchoPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("CatalystEchoPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public CatalystEchoPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;
        this.amount = amount;

        // 沿用原版 NightmarePower 的图标：牌的效果视觉上没变（还是"下回合给牌"），
        // 换成自绘图标反而会让玩家以为出了新东西。nightmare 是原版 powers.atlas 里的合法区域。
        loadRegion("nightmare");
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void atStartOfTurn() {
        // 一次性效果：结算完就移除自己（与原版 NightmarePower 行为一致）
        this.flash();
        for (int i = 0; i < this.amount; ++i) {
            // 用 EchoACardAction 生成，回响自带 虚无(Ethereal) + 消耗(Exhaust) + Echo 标签，
            // 与《震荡的回响》《怨灵回响》保持一致。
            addToBot(new EchoACardAction(new Catalyst()));
        }
        addToBot(new com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction(
                this.owner, this.owner, POWER_ID));
    }
}
