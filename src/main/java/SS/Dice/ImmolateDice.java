package SS.Dice;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInDiscardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.DiceDamageEnemyAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import basemod.cardmods.EtherealMod;
import basemod.cardmods.ExhaustMod;
import basemod.helpers.CardModifierManager;

public class ImmolateDice extends AbstractDice {
    public static final String ORB_ID = ModHelper.makePath("ImmolateDice");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    private static final float PI_DIV_16 = 0.19634955F;
    private static final float ORB_WAVY_DIST = 0.05F;
    private static final float PI_4 = 12.566371F;
    private static final float ORB_BORDER_SCALE = 1.2F;

    public ImmolateDice(int damage, AbstractMonster m) {
        this.ID = ORB_ID;
        this.img = ImageMaster.loadImage("img/dice/ImmolateDice.png");
        this.name = orbString.NAME;
        AbstractPlayer p = AbstractDungeon.player;

        for (AbstractCard c : p.discardPile.group) {
            if (c.cardID == "Burn") {
                damage += 2;
            }
        }

        for (AbstractCard c : p.drawPile.group) {
            if (c.cardID == "Burn") {
                damage += 2;
            }
        }

        for (AbstractCard c : p.hand.group) {
            if (c.cardID == "Burn") {
                damage += 2;
            }
        }
        this.baseEvokeAmount = damage;
        this.evokeAmount = this.baseEvokeAmount;
        this.channelAnimTimer = 0.5F;
        this.angle = MathUtils.random(360.0F);
        this.myColor = CardHelper.getColor(249, 0, 0);
        this.target = m;
        this.faces = 6;
        this.tags.add(AbstractCardEnum.AggresiveDice);
        updateDescription();
    }

    public void updateDescription() {
        this.description = orbString.DESCRIPTION[0] + this.evokeAmount + orbString.DESCRIPTION[1]
                + orbString.DESCRIPTION[2];
    }

    public void myEvoke() {
        int temp = result, damage = this.evokeAmount;
        // System.out.println(temp);
        AbstractDungeon.actionManager
                .addToBottom(new DiceDamageEnemyAction(damage, (AbstractMonster) this.target, true));
        AbstractCard c = new Burn();
        if (temp == 1) {
            c.upgrade();
        }
        if (temp == 6) {
            CardModifierManager.addModifier(c, new EtherealMod());
        }
        AbstractDungeon.actionManager
                .addToBottom(new MakeTempCardInDiscardAction(c, 1));
    }

    public AbstractDice makeCopy() {
        return new AttackDice(this.baseEvokeAmount, (AbstractMonster) this.target);
    }
}