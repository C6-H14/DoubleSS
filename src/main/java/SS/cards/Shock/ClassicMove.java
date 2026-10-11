package SS.cards.Shock;

import java.util.ArrayList;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.watcher.ChooseOneAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.animation.CharacterAnimationController;
import SS.cards.AbstractDoubleCard;
import SS.cards.Options.CommonCardOption;
import SS.cards.Options.RareCardOption;
import SS.cards.Options.UncommonCardOption;
import SS.helper.ModHelper;

public class ClassicMove extends AbstractShockCard {
    public static final String ID = ModHelper.makePath("ClassicMove");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/Shock/ClassicMove.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.SKILL;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.UNCOMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.SELF;

    public ClassicMove() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.exhaust = true;
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBaseCost(0);
            UpdateDescription();
            initializeDescription();
        }
    }

    /**
     * 强化后描述要写明"费用变为 0"。
     *
     * <p>cards.json 里 UPGRADE_DESCRIPTION 与基础描述逐字相同，玩家看不出强化差在哪。
     * 这里在运行时统一拼后缀，避免改本地化文件（改 json 要同步所有语言）。
     *
     * <p>升级费用这件事与"是否处于群主身份"无关，所以两种分支（基础文本 /
     * EXTENDED_DESCRIPTION）都要加。super 只负责把 rawDescription 设成对应分支的文本。
     */
    @Override
    public void UpdateDescription() {
        super.UpdateDescription();
        if (this.upgraded && this.rawDescription != null
                && !this.rawDescription.contains("费用变为0")) {
            this.rawDescription = this.rawDescription + " 费用变为0 。";
        }
    }

    /**
     * 进入/退出群主身份时同步状态位。
     *
     * <p>基类 {@code updateManager()} 只刷新描述、不改 {@code isManager}，
     * 构造时不在群主身份下的卡牌（{@code isManager == false}）之后永远进不了
     * {@code !isFiend && isManager} 分支，牌面会一直停在 DESCRIPTION 上。
     */
    @Override
    public void updateManager() {
        this.isManager = true;
        super.updateManager();
    }

    @Override
    public void exitManager() {
        this.isManager = false;
        super.exitManager();
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        ArrayList<AbstractCard> stanceChoices = new ArrayList<>();
        stanceChoices.add(new CommonCardOption());
        stanceChoices.add(new UncommonCardOption());
        stanceChoices.add(new RareCardOption());
        addToBot((AbstractGameAction) new ChooseOneAction(stanceChoices));
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.COMMAND_ALLY;
    }

    public AbstractDoubleCard makeCopy() {
        return new ClassicMove();
    }
}
