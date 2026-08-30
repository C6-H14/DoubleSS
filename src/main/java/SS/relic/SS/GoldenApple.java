package SS.relic.SS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.mod.stslib.relics.ClickableRelic;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardRarity;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.MonsterRoom;

import SS.action.common.EchoACardAction;
import SS.action.common.MessageCaller;
import SS.helper.ModHelper;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;
import basemod.abstracts.CustomRelic;

public class GoldenApple extends CustomRelic implements ClickableRelic {
    public static final String ID = ModHelper.makePath("GoldenApple");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/GoldenApple.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;

    public GoldenApple() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    public void atTurnStart() {
        addToBot(new EchoACardAction(generateCard()));
    }

    private AbstractCard generateCard() {
        int roll = AbstractDungeon.cardRandomRng.random(99);
        AbstractCard.CardRarity cardRarity;
        if (roll < 55) {
            cardRarity = CardRarity.COMMON;
        } else if (roll < 85) {
            cardRarity = CardRarity.UNCOMMON;
        } else {
            cardRarity = CardRarity.RARE;
        }
        ArrayList<AbstractCard> cardList = new ArrayList<>();
        for (AbstractPackage p : modcore.validPackage) {
            if (p.OptionCard.packagetype == PackageEnum.Hao) {
                for (AbstractCard c : p.CardLists) {
                    cardList.add(c);
                }
            }
        }
        Collections.shuffle(cardList, new Random(AbstractDungeon.cardRandomRng.randomLong()));
        AbstractCard tmp = cardList.get(0);
        for (AbstractCard c : cardList) {
            if (c.rarity == cardRarity) {
                tmp = c.makeCopy();
                break;
            }
        }
        AbstractCard derp = tmp.makeCopy();
        if (AbstractDungeon.player.hasRelic("Frozen Egg 2") && derp.type == CardType.POWER) {
            derp.upgrade();
        }
        if (AbstractDungeon.player.hasRelic("Molten Egg 2") && derp.type == CardType.ATTACK) {
            derp.upgrade();
        }
        if (AbstractDungeon.player.hasRelic("Toxic Egg 2") && derp.type == CardType.SKILL) {
            derp.upgrade();
        }

        return derp;
    }

    @Override
    public void onRightClick() {
        // 1. 安全检查：必须在战斗房间内右键才触发
        if (AbstractDungeon.getCurrRoom() instanceof MonsterRoom) {

            this.addToBot(new MessageCaller("Hao"));
        }
    }

    public AbstractRelic makeCopy() {
        return new GoldenApple();
    }
}
