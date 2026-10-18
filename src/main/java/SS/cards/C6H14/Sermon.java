package SS.cards.C6H14;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.animation.CharacterAnimationController;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.power.SermonPower;
import basemod.helpers.CardModifierManager;

public class Sermon extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Sermon");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Sermon.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public Sermon() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(1);
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            // 强化只加"保留"，不加 !M! —— 数值成长走群主身份的染色，见 EXTENDED_DESCRIPTION。
            this.selfRetain = true;
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        // 挂上本回合的布道记录。之后的每次出牌由 SermonPower.onUseCard 负责判定与唤魂。
        selfPower(new SermonPower(p, this.magicNumber));
    }

    /**
     * 群主身份：本牌获得"染色"。
     *
     * <p>与 {@link Hymn} 同款做法 —— 挂 {@link PaintingModifier}，打出时自动施加
     * {@link SS.power.PaintingPower}，于是本回合后续出牌会改变魂火颜色。
     *
     * <p>注意必须调 {@code super}：基类负责置 {@code isManager} 并刷新描述文本，
     * 少了它牌面不会切到 EXTENDED_DESCRIPTION。
     */
    @Override
    public void updateManager() {
        super.updateManager();
        CardModifierManager.addModifier(this, new PaintingModifier());
    }

    @Override
    public void exitManager() {
        super.exitManager();
        CardModifierManager.removeSpecificModifier(this, new PaintingModifier(), false);
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.CAST_PRAYER;
    }

    public AbstractDoubleCard makeCopy() {
        return new Sermon();
    }
}
