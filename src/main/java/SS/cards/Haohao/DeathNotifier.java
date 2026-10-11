package SS.cards.Haohao;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.green.Catalyst;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.PoisonPower;

import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.helper.ModHelper;
import SS.path.AbstractCardEnum;

public class DeathNotifier extends AbstractHaoCard {
    public static final String ID = ModHelper.makePath("DeathNotifier");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Haohao/DeathNotifier.png";
    private static final int COST = 2;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardColor COLOR = AbstractCardEnum.Hao_Green;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ALL_ENEMY;

    public DeathNotifier() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, COLOR, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.magicNumber = this.baseMagicNumber = 4;
        // 预览与实际获得的是同一张牌（回响版催化剂），用同一个构造入口避免再次跑偏
        this.cardsToPreview = SS.action.common.EchoACardAction.buildEcho(new Catalyst(), false);
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeMagicNumber(2);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        if (needManager() && !AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            for (final AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
                if (!monster.isDead && !monster.isDying) {
                    this.addToBot(new ApplyPowerAction(monster, p, new PoisonPower(monster, p, this.magicNumber),
                            this.magicNumber));
                }
            }
        }
        // 【回响版催化剂】原版 NightmarePower 塞的是普通 Catalyst（MakeTempCardInHandAction），
        // 这里改用 CatalystEchoPower，在回合开始时通过 EchoACardAction 生成【催化剂的回响】
        // （虚无 + 消耗 + Echo 标签），与《震荡的回响》《怨灵回响》同一套机制。
        addToBot(new ApplyPowerAction(p, p, new SS.power.CatalystEchoPower(p, upgraded ? 4 : 3)));
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.CAST_DEBUFF;
    }

    public AbstractDoubleCard makeCopy() {
        return new DeathNotifier();
    }
}
