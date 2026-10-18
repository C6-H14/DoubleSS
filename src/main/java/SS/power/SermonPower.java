package SS.power;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

import SS.action.monster.EvokeSoulAction;
import SS.helper.CardColorHelper;
import SS.helper.ModHelper;

/**
 * 布道：本回合每当你打出牌时，若本回合未打出过同色牌，唤魂 {@link #amount}。
 *
 * <p>本能力只存活一个回合（{@link #atEndOfTurn} 自删），所以"本回合"的记录直接放在
 * 实例字段 {@link #playedColorKeys} 里，不需要额外的回合清理逻辑 —— 能力没了记录也就没了。
 *
 * <p>"同色"的口径见 {@link CardColorHelper#isSameColor}：SS_Yellow 牌比卡包类型，
 * 其余比原版颜色。
 */
public class SermonPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("SermonPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    /** 本回合已经打出过的"色组"（{@link CardColorHelper#colorKey} 的结果）。 */
    private final Set<Integer> playedColorKeys = new HashSet<>();

    /**
     * @param evokeAmount 每次触发要唤魂的点数（对应卡面的 !M!）
     */
    public SermonPower(AbstractCreature owner, int evokeAmount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = evokeAmount;

        String path128 = "img/power/SermonPower84.png";
        String path48 = "img/power/SermonPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    @Override
    public void onUseCard(final AbstractCard card, final UseCardAction action) {
        if (card == null) {
            return;
        }
        // 打出这张牌之前，本回合有没有出现过同色牌？
        // 用 colorKey 而不是遍历比 isSameColor：Set 查一次就够，且语义完全等价。
        int key = CardColorHelper.colorKey(card);
        boolean firstOfColor = this.playedColorKeys.add(key);
        if (!firstOfColor) {
            return;
        }

        // 这张牌是它这一色的第一张 —— 触发唤魂。
        this.flash();
        this.addToBot(new EvokeSoulAction(this.amount));
    }

    @Override
    public void atEndOfTurn(boolean isPlayer) {
        // 布道只持续本回合。记录随能力一起消失，天然满足"每回合重新计数"。
        this.addToBot(new RemoveSpecificPowerAction(this.owner, this.owner, this));
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}
