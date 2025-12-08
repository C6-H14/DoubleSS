package SS.power;

import SS.cards.PeptideStrike;
import SS.helper.ModHelper;

import java.util.Iterator;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.common.DamageAction;

public class PepsinPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("PepsinPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public PepsinPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/PepsinPower84.png";
        String path48 = "img/power/PepsinPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    public void stackPower(int stackAmount) {
        this.fontScale = 8.0F;
        this.amount += stackAmount;
        this.updateExistingPeptide();
    }

    private void updateExistingPeptide() {
        Iterator var1 = AbstractDungeon.player.hand.group.iterator();

        AbstractCard c;
        while (var1.hasNext()) {
            c = (AbstractCard) var1.next();
            if (c instanceof PeptideStrike) {
                if (!c.upgraded) {
                    c.baseDamage = 2 + this.amount;
                } else {
                    c.baseDamage = 3 + this.amount;
                }
            }
        }

        var1 = AbstractDungeon.player.drawPile.group.iterator();

        while (var1.hasNext()) {
            c = (AbstractCard) var1.next();
            if (c instanceof PeptideStrike) {
                if (!c.upgraded) {
                    c.baseDamage = 2 + this.amount;
                } else {
                    c.baseDamage = 3 + this.amount;
                }
            }
        }

        var1 = AbstractDungeon.player.discardPile.group.iterator();

        while (var1.hasNext()) {
            c = (AbstractCard) var1.next();
            if (c instanceof PeptideStrike) {
                if (!c.upgraded) {
                    c.baseDamage = 2 + this.amount;
                } else {
                    c.baseDamage = 3 + this.amount;
                }
            }
        }

        var1 = AbstractDungeon.player.exhaustPile.group.iterator();

        while (var1.hasNext()) {
            c = (AbstractCard) var1.next();
            if (c instanceof PeptideStrike) {
                if (!c.upgraded) {
                    c.baseDamage = 2 + this.amount;
                } else {
                    c.baseDamage = 3 + this.amount;
                }
            }
        }

    }

    public void onDrawOrDiscard() {
        Iterator var1 = AbstractDungeon.player.hand.group.iterator();

        while (var1.hasNext()) {
            AbstractCard c = (AbstractCard) var1.next();
            if (c instanceof PeptideStrike) {
                if (!c.upgraded) {
                    c.baseDamage = 2 + this.amount;
                } else {
                    c.baseDamage = 3 + this.amount;
                }
            }
        }

    }

}
