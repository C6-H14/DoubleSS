package SS.cards.C6H14;

import java.util.ArrayList;

import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;

import SS.Dice.AttackDice;
import SS.action.dice.ChannelDiceAction;
import SS.animation.CharacterAnimationController;
import SS.cardmodifiers.PaintingModifier;
import SS.cards.AbstractDoubleCard;
import SS.cards.Haohao.AbstractHaoCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.power.InscribeCardPower;
import basemod.helpers.CardModifierManager;

public class BlazingStrike extends AbstractC6H14Card {
    public static final String ID = ModHelper.makePath("BlazingStrike");
    private static final CardStrings CARD_STRINGS = CardCrawlGame.languagePack.getCardStrings(ID);
    private static final String NAME = CARD_STRINGS.NAME;
    private static final String IMG_PATH = "img/cards/C6H14/BlazingStrike.png";
    private static final int COST = 1;
    private static final String DESCRIPTION = CARD_STRINGS.DESCRIPTION;
    private static final AbstractCard.CardType TYPE = AbstractCard.CardType.ATTACK;
    private static final AbstractCard.CardRarity RARITY = AbstractCard.CardRarity.COMMON;
    private static final AbstractCard.CardTarget TARGET = AbstractCard.CardTarget.ENEMY;

    public BlazingStrike() {
        super(ID, NAME, IMG_PATH, COST, DESCRIPTION, TYPE, RARITY, TARGET, CARD_STRINGS,
                CARD_STRINGS.EXTENDED_DESCRIPTION);
        this.tags.add(CardTags.STRIKE);
        setDamage(6);
        if (needManager()) {
            updateManager();
        }
        UpdateDescription();
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(3);
            UpdateDescription();
            initializeDescription();
        }
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new ChannelDiceAction(new AttackDice(damage, m)));
        ModHelper.atbLambda(() -> {
            ArrayList<AbstractHaoCard> candidates = new ArrayList<>();
            for (AbstractCard c : p.hand.group) {
                if (c instanceof AbstractHaoCard) {
                    candidates.add((AbstractHaoCard) c);
                }
            }

            if (candidates.isEmpty()) {
                return;
            }

            AbstractHaoCard target = candidates.get(AbstractDungeon.cardRandomRng.random(candidates.size() - 1));
            target.superFlash();

            AbstractCard copy = target.makeStatEquivalentCopy();

            InscribeCardPower power = null;
            SoulAlly carrier = null;
            for (AbstractMonster mo : AllyManager.allies.monsters) {
                if (mo instanceof SoulAlly && !mo.isDeadOrEscaped() && mo.hasPower("Double:InscribeCardPower")) {
                    carrier = (SoulAlly) mo;
                    power = (InscribeCardPower) mo.getPower("Double:InscribeCardPower");
                    break;
                }
            }
            if (power == null && p.hasPower("Double:InscribeCardPower")) {
                power = (InscribeCardPower) p.getPower("Double:InscribeCardPower");
                carrier = null; // 铭刻在玩家身上，不涉及魂火染色
            }

            if (power == null) {
                return;
            }

            // 1. 先打出旧牌。这里必须补一个 WaitAction：power.playCard() 只是把
            //    PlayCardAction 排进队列，紧接着的 setCard 会把 power.card 覆盖掉，
            //    如果中间不等一拍，旧牌的"飞出去 -> 结算"会和"新牌装进槽位"挤在同一帧，
            //    观感上就是旧牌被吞掉了。SoulColorCycleAction 用的也是同样的 0.3f 间隔。
            if (power.card != null) {
                AbstractCard visualCard = power.card.makeStatEquivalentCopy();
                visualCard.current_x = (carrier != null) ? carrier.hb.cX : p.hb.cX;
                visualCard.current_y = (carrier != null) ? carrier.hb.cY : p.hb.cY;
                AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(visualCard));

                power.playCard();
                addToTop(new WaitAction(0.3f));
            }

            // 2. 再铭刻新牌。走 setCard 而不是直接写字段，保证描述同步。
            copy.setCostForTurn(0);
            copy.retain = true;
            power.setCard(copy);

            // 3. 魂火切换为被铭刻牌对应的颜色（不额外吸牌）。
            if (carrier != null) {
                carrier.setColorOnly(soulColorOf(copy));
            }
        });
    }

    /** 卡牌类型 → 魂火颜色。与 SoulColorCycleAction / PaintingPower 的映射一致。 */
    private static SoulAlly.SoulColor soulColorOf(AbstractCard c) {
        switch (c.type) {
            case ATTACK:
                return SoulAlly.SoulColor.RED;
            case SKILL:
                return SoulAlly.SoulColor.GREEN;
            case POWER:
                return SoulAlly.SoulColor.BLUE;
            default:
                // STATUS / CURSE 之类一律紫色，与 SoulColorCycleAction 第 63 行的处理相同
                return SoulAlly.SoulColor.PURPLE;
        }
    }

    public void updateManager() {
        super.updateManager();
        CardModifierManager.addModifier(this, new PaintingModifier());
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        super.exitManager();
        CardModifierManager.removeModifiersById(this, "Double:PaintingModifier", false);
        UpdateDescription();
        initializeDescription();
    }

    @Override
    public String getDefaultCharacterAnimation() {
        return CharacterAnimationController.ATTACK_SWORD;
    }

    public AbstractDoubleCard makeCopy() {
        return new BlazingStrike();
    }
}
