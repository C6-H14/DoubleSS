package SS.cards.C6H14;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.PoisonPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;

import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;

public class Conduct extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("Conduct");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/Conduct.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public Conduct() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            this.selfRetain = true;
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        ModHelper.atbLambda(() -> {
            // 1. 统计场上所有魂火的数值总和
            int totalAmount = 0;
            for (AbstractMonster mo : AllyManager.allies.monsters) {
                if (mo instanceof SoulAlly && !mo.isDeadOrEscaped()) {
                    AbstractPower p1 = mo.getPower("Double:SoulFirePower");
                    if (p1 != null)
                        totalAmount = Math.max(totalAmount, p1.amount);

                }
            }

            // 2. 每拥有 1 点，对随机敌人施加随机 Debuff
            for (int i = 0; i < totalAmount; i++) {
                AbstractMonster randomMonster = AbstractDungeon.getMonsters().getRandomMonster(null, true,
                        AbstractDungeon.cardRandomRng);
                if (randomMonster != null) {
                    applyRandomDebuff(randomMonster, p);
                }
            }

            // 3. needManager() 额外对所有敌人施加 1 层易伤和 1 层虚弱
            if (needManager()) {
                for (AbstractMonster enemy : AbstractDungeon.getCurrRoom().monsters.monsters) {
                    if (!enemy.isDeadOrEscaped()) {
                        addToTop(new ApplyPowerAction(enemy, p, new WeakPower(enemy, 1, false), 1));
                        addToTop(new ApplyPowerAction(enemy, p, new VulnerablePower(enemy, 1, false), 1));
                    }
                }
            }
        });
    }

    private void applyRandomDebuff(AbstractMonster target, AbstractPlayer p) {
        int roll = AbstractDungeon.cardRandomRng.random(2);
        switch (roll) {
            case 0:
                addToTop(new ApplyPowerAction(target, p, new VulnerablePower(target, 1, false), 1));
                break;
            case 1:
                addToTop(new ApplyPowerAction(target, p, new WeakPower(target, 1, false), 1));
                break;
            case 2:
                addToTop(new ApplyPowerAction(target, p, new PoisonPower(target, p, 2), 2));
                break;
        }
    }

    public AbstractDoubleCard makeCopy() {
        return new Conduct();
    }
}