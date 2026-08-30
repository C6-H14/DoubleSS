package SS.cards.Lost;

import java.util.ArrayList;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.action.common.PlayCardAction;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;
import SS.power.DyingPower;

public class GhostBomb extends AbstractLostCard {
    public static final String ID = ModHelper.makePath("GhostBomb");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Lost/GhostBomb.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Lost_Black;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public GhostBomb() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.tags.add(AbstractCardEnum.Permanent);
        this.isEthereal = true;
        // 初始 14 点 permanentDamage
        this.permanentDamage = this.basePermanentDamage = 14;
        this.permanentMagicNumber = this.basePermanentMagicNumber = 1;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            // 升级 +4 点，达到 18 点
            upgradePermanentDamage(4);
            UpdateDescription();
            initializeDescription();
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        // 1. 对所有敌人造成 permanentDamage 伤害
        int[] damages = DamageInfo.createDamageMatrix(this.permanentDamage, true);
        addToBot(new DamageAllEnemiesAction(p, damages, DamageInfo.DamageType.NORMAL,
                AbstractGameAction.AttackEffect.FIRE));

        // 2. 随机打出手牌中的 1 张虚无牌（排除这张牌自身）
        ArrayList<AbstractCard> etherealCards = new ArrayList<>();
        for (AbstractCard c : p.hand.group) {
            if (c.isEthereal && c != this) {
                etherealCards.add(c);
            }
        }

        if (!etherealCards.isEmpty()) {
            AbstractCard cardToPlay = etherealCards.get(AbstractDungeon.cardRandomRng.random(etherealCards.size() - 1));
            // 从手牌中移出
            p.hand.removeCard(cardToPlay);

            // 寻找随机存活敌人作为指向性目标的备选
            AbstractMonster targetMonster = AbstractDungeon.getCurrRoom().monsters.getRandomMonster(null, true,
                    AbstractDungeon.cardRandomRng);

            // 使用之前封装的 PlayCardAction 打出
            addToBot(new PlayCardAction(targetMonster, cardToPlay, false));
        }

        // 3. 若 needManager() 则给予 1 层 DyingPower
        if (needManager()) {
            addToBot(new ApplyPowerAction(p, p, new DyingPower(p, 1), 1));
        }
    }

    @Override
    public AbstractDoubleCard makeCopy() {
        GhostBomb c = new GhostBomb();
        c.copyPermanentFieldsFrom(this);
        return c;
    }
}