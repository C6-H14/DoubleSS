package SS.rewards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardRarity;
import com.megacrit.cardcrawl.cards.AbstractCard.CardType;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.rewards.RewardItem;

import SS.helper.ModHelper;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.path.RewardEnum;
import SS.path.PackageEnumList.PackageEnum;
import basemod.abstracts.CustomReward;

public class HaoReward extends CustomReward {
    public static final String ID = ModHelper.makePath("HaoReward");
    public static final String[] TEXT = CardCrawlGame.languagePack.getUIString(HaoReward.ID).TEXT;
    private static final Texture ICON = new Texture(Gdx.files.internal("img/reward/HaoReward.png"));
    public ArrayList<AbstractCard> cards = new ArrayList<>();

    public HaoReward() {
        super(ICON, Description(), RewardEnum.HaoCardReward);
        this.cards.clear();
        this.cards.add(generateCard());
    }

    public HaoReward(String cardid) {
        super(ICON, Description(), RewardEnum.HaoCardReward);
        this.cards.clear();
        if (cardid == null) {
            this.cards.add(generateCard());
            return;
        }
        AbstractCard derp = CardLibrary.getCard(cardid).makeCopy();
        if (AbstractDungeon.player.hasRelic("Frozen Egg 2") && derp.type == CardType.POWER) {
            derp.upgrade();
        }
        if (AbstractDungeon.player.hasRelic("Molten Egg 2") && derp.type == CardType.ATTACK) {
            derp.upgrade();
        }
        if (AbstractDungeon.player.hasRelic("Toxic Egg 2") && derp.type == CardType.SKILL) {
            derp.upgrade();
        }
        this.cards.add(derp);
    }

    private static String Description() {
        return TEXT[0];
    }

    private AbstractCard generateCard() {
        int roll = AbstractDungeon.cardRng.random(99);
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
        Collections.shuffle(cardList, new Random(AbstractDungeon.cardRng.randomLong()));
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

    public boolean claimReward() {
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
            AbstractDungeon.cardRewardScreen.open(this.cards, (RewardItem) this, HaoReward.TEXT[1]);
            AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
        }
        return false;
    }
}
