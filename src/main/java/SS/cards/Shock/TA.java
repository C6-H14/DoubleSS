package SS.cards.Shock;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;

import SS.action.common.DamageFatalAction;
import SS.cards.AbstractDoubleCard;
import SS.cards.Haohao.AbstractHaoCard;
import SS.helper.ModHelper;
import SS.helper.SynergismGraph.SynTag;
import SS.modcore.modcore;
import SS.path.PackageEnumList.PackageEnum;

public class TA extends AbstractShockCard {
    public static final String ID = ModHelper.makePath("TA");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/NoImage_attack.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.RARE;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public TA() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.damage = this.baseDamage = 13;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(5);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        DamageInfo info = new DamageInfo(p, this.damage, DamageType.NORMAL);
        Consumer<DamageInfo> c;
        if (needManager()) {
            c = message -> {
                int cnt = 0;
                for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
                    if (card.canUpgrade()) {
                        ++cnt;
                    }
                }
                if (cnt == 0)
                    return;
                ArrayList<AbstractCard> possibleCards = new ArrayList<>();
                for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
                    if (card instanceof AbstractDoubleCard) {
                        if (card.canUpgrade() && modcore.synGraph.hasSyn(PackageEnum.Shock,
                                ((AbstractDoubleCard) card).packagetype, SynTag.Student)) {
                            possibleCards.add(card);
                        }
                    }
                }
                if (!possibleCards.isEmpty()) {
                    AbstractCard card = possibleCards
                            .get(AbstractDungeon.miscRng.random(0, possibleCards.size() - 1));
                    card.upgrade();
                    AbstractDungeon.player.bottledCardUpgradeCheck(card);
                    card.upgrade();
                    float x = MathUtils.random(0.1F, 0.9F) * Settings.WIDTH;
                    float y = MathUtils.random(0.2F, 0.8F) * Settings.HEIGHT;
                    AbstractDungeon.effectList.add(new ShowCardBrieflyEffect(card.makeStatEquivalentCopy(), x, y));
                    AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect(x, y));
                }
            };
        } else {
            c = message -> {
                int cnt = 0;
                for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
                    if (card.canUpgrade()) {
                        ++cnt;
                    }
                }
                if (cnt == 0)
                    return;
                ArrayList<AbstractCard> possibleCards = new ArrayList<>();
                for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
                    if (card instanceof AbstractHaoCard && card.canUpgrade()) {
                        possibleCards.add(card);
                    }
                }
                if (!possibleCards.isEmpty()) {
                    AbstractCard card = possibleCards
                            .get(AbstractDungeon.miscRng.random(0, possibleCards.size() - 1));
                    card.upgrade();
                    AbstractDungeon.player.bottledCardUpgradeCheck(card);
                    card.upgrade();
                    float x = MathUtils.random(0.1F, 0.9F) * Settings.WIDTH;
                    float y = MathUtils.random(0.2F, 0.8F) * Settings.HEIGHT;
                    AbstractDungeon.effectList.add(new ShowCardBrieflyEffect(card.makeStatEquivalentCopy(), x, y));
                    AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect(x, y));
                }
            };
        }
        addToBot(new DamageFatalAction(m, info, c));
    }

    @Override
    public boolean hasSyn() {
        if (needManager()) {
            return modcore.synGraph.hasAnySynInDeck(PackageEnum.Shock, SynTag.Student, AbstractDungeon.player);
        }
        for (AbstractCard card : AbstractDungeon.player.masterDeck.group) {
            if (card instanceof AbstractHaoCard) {
                return true;
            }
        }
        return false;
    }

    public AbstractDoubleCard makeCopy() {
        return new TA();
    }
}
