package SS.rewards;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardRarity;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

import basemod.ReflectionHacks;
import basemod.abstracts.CustomReward;

public class CommonCardReward {
    public static AbstractPlayer player;

    public CommonCardReward() {
    }

    public void update() {
        player = AbstractDungeon.player;
    }

    public static ArrayList<AbstractCard> getRewardCards() {
        ArrayList<AbstractCard> retVal = new ArrayList();
        int numCards = 3;

        AbstractRelic r;
        for (Iterator var2 = player.relics.iterator(); var2
                .hasNext(); numCards = r.changeNumberOfCardsInReward(numCards)) {
            r = (AbstractRelic) var2.next();
            System.out.println("Debugging:" + r.relicId + ":" + numCards);
        }

        if (ModHelper.isModEnabled("Binary")) {
            --numCards;
        }

        AbstractCard card;
        for (int i = 0; i < numCards; ++i) {
            AbstractCard.CardRarity rarity = rollRarity();
            card = null;
            switch (rarity) {
                case RARE: {
                    AbstractDungeon.cardBlizzRandomizer = AbstractDungeon.cardBlizzStartOffset;
                    break;
                }
                case UNCOMMON: {
                    break;
                }
                case COMMON: {
                    AbstractDungeon.cardBlizzRandomizer -= AbstractDungeon.cardBlizzGrowth;
                    if (AbstractDungeon.cardBlizzRandomizer <= AbstractDungeon.cardBlizzMaxOffset) {
                        AbstractDungeon.cardBlizzRandomizer = AbstractDungeon.cardBlizzMaxOffset;
                        break;
                    }
                    break;
                }
                default: {
                    break;
                }
            }
            boolean containsDupe = true;

            while (true) {
                while (containsDupe) {
                    containsDupe = false;
                    if (player.hasRelic("PrismaticShard")) {
                        card = CardLibrary.getAnyColorCard(rarity);
                    } else {
                        card = getCard(rarity);
                    }

                    Iterator var6 = retVal.iterator();

                    while (var6.hasNext()) {
                        AbstractCard c = (AbstractCard) var6.next();
                        if (c.cardID.equals(card.cardID)) {
                            containsDupe = true;
                            break;
                        }
                    }
                }

                if (card != null) {
                    retVal.add(card);
                }
                break;
            }
        }

        ArrayList<AbstractCard> retVal2 = new ArrayList();
        Iterator var11 = retVal.iterator();

        while (var11.hasNext()) {
            card = (AbstractCard) var11.next();
            retVal2.add(card.makeCopy());
        }

        var11 = retVal2.iterator();

        while (true) {
            while (var11.hasNext()) {
                card = (AbstractCard) var11.next();
                if (card.rarity != CardRarity.RARE && AbstractDungeon.cardRng.randomBoolean(upgradeChance())
                        && card.canUpgrade()) {
                    card.upgrade();
                } else {
                    Iterator var12 = player.relics.iterator();

                    while (var12.hasNext()) {
                        r = (AbstractRelic) var12.next();
                        r.onPreviewObtainCard(card);
                    }
                }
            }

            return retVal2;
        }
    }

    public static AbstractCard getCard(final AbstractCard.CardRarity rarity) {
        switch (rarity) {
            case RARE: {
                return AbstractDungeon.rareCardPool.getRandomCard(true);
            }
            case UNCOMMON: {
                return AbstractDungeon.uncommonCardPool.getRandomCard(true);
            }
            case COMMON: {
                return AbstractDungeon.commonCardPool.getRandomCard(true);
            }
            case CURSE: {
                return AbstractDungeon.curseCardPool.getRandomCard(true);
            }
            default: {
                return null;
            }
        }
    }

    public static AbstractCard.CardRarity rollRarity() {
        return rollRarity(AbstractDungeon.cardRng);
    }

    public static AbstractCard.CardRarity rollRarity(Random rng) {
        int roll = AbstractDungeon.cardRng.random(99);
        roll += AbstractDungeon.cardBlizzRandomizer;
        return getCardRarityFallback(roll);
    }

    public static AbstractCard.CardRarity getCardRarityFallback(int roll) {
        int rareRate = 3;
        if (roll < rareRate) {
            return CardRarity.RARE;
        } else {
            return roll < 40 ? CardRarity.UNCOMMON : CardRarity.COMMON;
        }
    }

    public static float upgradeChance() {
        if (CardCrawlGame.dungeon instanceof Exordium) {
            return 0.0F;
        }
        if (CardCrawlGame.dungeon instanceof TheCity) {
            if (AbstractDungeon.ascensionLevel >= 12) {
                return 0.125F;
            } else {
                return 0.25F;
            }
        }
        if (CardCrawlGame.dungeon instanceof TheBeyond) {
            if (AbstractDungeon.ascensionLevel >= 12) {
                return 0.25F;
            } else {
                return 0.5F;
            }
        }
        if (AbstractDungeon.ascensionLevel >= 12) {
            return 0.25F;
        } else {
            return 0.5F;
        }
    }
}
