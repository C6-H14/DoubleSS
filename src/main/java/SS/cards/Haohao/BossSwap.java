package SS.cards.Haohao;

import java.util.ArrayList;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.PoisonPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.AbstractRelic.RelicTier;

import SS.action.unique.haohao.TempRemoveRelicAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.path.AbstractCardEnum;

public class BossSwap extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("BossSwap");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/BossSwap.png";
    private static final int COST = 0;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.BASIC;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public BossSwap() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.magicNumber = this.baseMagicNumber = 2;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(2);
            if (this.cardsToPreview != null) {
                this.cardsToPreview.upgrade();
            }
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (!mo.isDeadOrEscaped()) {
                addToBot(new ApplyPowerAction(mo, p, new PoisonPower(mo, p, this.magicNumber), this.magicNumber));
            }
        }

        // 2. 查找所有满足条件的遗物
        int amount = 0;
        ArrayList<AbstractRelic> validRelics = new ArrayList<>();

        for (AbstractRelic r : p.relics) {
            boolean isTarget = false;
            if (r.tier == RelicTier.STARTER) {
                isTarget = true;
            } else {
                for (AbstractPackage c : modcore.mainPackageList) {
                    if (c.StartRelic != null && c.StartRelic.relicId.equals(r.relicId)) {
                        isTarget = true;
                        break;
                    }
                }
            }

            if (isTarget) {
                ++amount;
                validRelics.add(r);
            }
        }

        // 3. 在本场战斗中随机移除一个满足条件的遗物
        if (!validRelics.isEmpty()) {
            AbstractRelic toRemove = validRelics.get(AbstractDungeon.cardRandomRng.random(validRelics.size() - 1));
            addToBot(new TempRemoveRelicAction(toRemove));
            amount = Math.max(0, amount - 1);
        }
        // 5. 所有敌人获得 magicNumber * amount 层 PoisonPower
        int totalPoison = (4 - amount);
        if (totalPoison > 0 && needManager()) {
            for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!mo.isDeadOrEscaped()) {
                    addToBot(new ApplyPowerAction(mo, p, new PoisonPower(mo, p, totalPoison), totalPoison));
                }
            }
        }
    }

    @Override
    public AbstractDoubleCard makeCopy() {
        return new BossSwap();
    }
}