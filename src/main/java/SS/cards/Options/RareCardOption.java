package SS.cards.Options;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.common.DiscoveryByRarityAction;
import SS.helper.ModHelper;
import basemod.abstracts.CustomCard;

public class RareCardOption extends CustomCard {
    public static final String ID = ModHelper.makePath("RareCardOption");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;

    public RareCardOption() {
        super(ID, NAME, "img/cards/NoImage_skill.png", -2, DESCRIPTION, CardType.SKILL,
                CardColor.COLORLESS, CardRarity.SPECIAL, CardTarget.NONE);
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        this.onChoseThisOption();
    }

    public void onChoseThisOption() {
        AbstractPlayer p = AbstractDungeon.player;
        addToBot(new DiscoveryByRarityAction(CardRarity.RARE,
                (AbstractDungeon.player != null && AbstractDungeon.player.getPower("Double:ManagerStance") != null
                        && !(AbstractDungeon.getCurrRoom()).isBattleEnding()) ? 4 : 3));
    }

    public void upgrade() {
    }

    public AbstractCard makeCopy() {
        return new RareCardOption();
    }
}
