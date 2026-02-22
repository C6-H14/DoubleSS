package SS.cardmodifiers;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;

import SS.helper.ModHelper;
import SS.power.PaintingPower;
import basemod.abstracts.AbstractCardModifier;

public class PaintingModifier extends AbstractCardModifier {
    public static String ID = ModHelper.makePath("PaintingModifier");
    private static final UIStrings STRINGS = CardCrawlGame.languagePack.getUIString(ID);

    // 修改描述
    @Override
    public String modifyDescription(String rawDescription, AbstractCard card) {
        return String.format(STRINGS.TEXT[0], rawDescription);
    }

    @Override
    public AbstractCardModifier makeCopy() {
        return new PaintingModifier();
    }

    @Override
    public void onUse(AbstractCard card, AbstractCreature target, UseCardAction action) {
        AbstractPlayer p = AbstractDungeon.player;
        addToBot(new ApplyPowerAction(p, p, new PaintingPower(p, 1)));
    }

    // modifier的ID
    @Override
    public String identifier(AbstractCard card) {
        return ID;
    }
}