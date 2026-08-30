package SS.relic.SS;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import basemod.abstracts.CustomRelic;

public class RGBPCup extends CustomRelic {
    public static final String ID = ModHelper.makePath("RGBPCup");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/RGBPCup.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.BOSS;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.MAGICAL;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private ArrayList<String> colorList = new ArrayList<>();

    public RGBPCup() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    private String getID(AbstractCard c) {
        if (c.color != AbstractCardEnum.SS_Yellow) {
            return c.color.toString();
        }
        AbstractDoubleCard card = (AbstractDoubleCard) c;
        return card.packagetype.toString();
    }

    public void atBattleStart() {
        colorList.clear();
    }

    public void onUseCard(AbstractCard targetCard, UseCardAction useCardAction) {
        if (!colorList.isEmpty() && colorList.contains(getID(targetCard))) {
            return;
        }
        this.flash();
        addToBot(new GainEnergyAction(1));
        addToBot(new DrawCardAction(1));
        colorList.add(getID(targetCard));
    }

    public AbstractRelic makeCopy() {
        return new RGBPCup();
    }
}
