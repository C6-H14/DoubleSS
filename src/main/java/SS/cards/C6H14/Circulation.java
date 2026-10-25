package SS.cards.C6H14;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.power.PaintingPower;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.DrawCardNextTurnPower;

public class Circulation extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Circulation");
    private static final CardStrings STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);

    public Circulation() {
        super(ID, STRINGS.NAME, "img/cards/C6H14/Circulation.png", 2,
                STRINGS.DESCRIPTION, CardType.ATTACK, CardRarity.COMMON, CardTarget.ENEMY,
                STRINGS, STRINGS.EXTENDED_DESCRIPTION);
        setDamage(8);
        setMagic(2);
        UpdateDescription();
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        selfPower(new PaintingPower(p, magicNumber));
        if (needManager()) {
            selfPower(new DrawCardNextTurnPower(p, 1));
        }
    }

    @Override
    public void upgrade() {
        if (!upgraded) {
            upgradeName();
            upgradeDamage(1);
            upgradeMagicNumber(1);
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public AbstractDoubleCard makeCopy() {
        return new Circulation();
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.CAST_ATTACK_2;
    }
}
