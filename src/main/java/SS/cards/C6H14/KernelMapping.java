package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.unique.c6h14.KernelMappingAction;
import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;

public class KernelMapping extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("KernelMapping");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/KernelMapping.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public KernelMapping() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        setMagic(2);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        // 参数 1 =「是否处于群主身份」：群主下改为让玩家指定要加费的牌，非群主则随机。
        // 参数 2 =「是否强化」：强化放宽可选费用上限（0 → 0或1），与群主身份无关。
        addToBot(new KernelMappingAction(needManager(), this.upgraded));
        addToBot(new GainEnergyAction(this.magicNumber));
    }

    public AbstractDoubleCard makeCopy() {
        return new KernelMapping();
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.CAST_ATTACK_1;
    }
}
