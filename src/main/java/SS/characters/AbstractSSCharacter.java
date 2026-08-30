package SS.characters;

import java.util.ArrayList;
import java.util.Collections;

import basemod.abstracts.CustomPlayer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.defect.AnimateOrbAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.defect.EvokeOrbAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;

import SS.Dice.EmptyDiceSlot;
import SS.cards.AbstractDoubleCard;
import SS.cards.MultiFacial;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

/**
 * 二硫键系角色基类。
 *
 * 把所有"机制"（骰子/充能球充能、卡包卡池、选角色特效、罪值条相关、
 * 起始事件卡、斯派尔之心攻击动作等）收敛到这一层，具体角色（MyCharacter
 * 及未来的变种）只提供身份差异：颜色枚举、起始牌组/圣物、本地化名称、
 * cutscene、@SpireEnum 注册等。
 *
 * 凡是判断"是否二硫键系"的地方（回合结束自动激发、颢牌掉落、卡包选择流程、
 * 罪值条渲染、人物选择自定义卡包 UI）都应 {@code instanceof AbstractSSCharacter}
 * 而不是某个具体角色，这样新变种自动继承整套机制，无需再改这些门槛。
 */
public abstract class AbstractSSCharacter extends CustomPlayer {
    /**
     * 转发给 CustomPlayer 的构造器（orbTextures/orbVfx/layerSpeed 那套带充能球动画的签名），
     * 子类 super(...) 一行不用改即可挂到基类。
     */
    protected AbstractSSCharacter(String name, AbstractPlayer.PlayerClass playerClass,
            String[] orbTextures, String orbVfxUrl, float[] layerSpeed, String cutscene, String relicList) {
        super(name, playerClass, orbTextures, orbVfxUrl, layerSpeed, cutscene, relicList);
    }

    public int getAscensionMaxHPLoss() {
        return 7;
    }

    public AbstractCard getStartCardForEvent() {
        return new MultiFacial();
    }

    public BitmapFont getEnergyNumFont() {
        return FontHelper.energyNumFontBlue;
    }

    public void doCharSelectScreenSelectEffect() {
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }

    @Override
    public void channelOrb(AbstractOrb orbToSet)// 产生骰子
    {
        if (orbToSet instanceof EmptyOrbSlot)
            return; // EmptyDiceSlot 继承 EmptyOrbSlot，一个判断同时覆盖两种空槽
        // 骰子和原版充能球都允许充能：回合结束时由 EvokeAllPatch 统一自动激发
        if (this.maxOrbs <= 0) {
            AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0F, MSG[4], true));
        } else {
            int index = -1;
            for (int i = 0; i < this.orbs.size(); i++) {
                if (this.orbs.get(i) instanceof EmptyOrbSlot) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                orbToSet.cX = ((AbstractOrb) this.orbs.get(index)).cX;
                orbToSet.cY = ((AbstractOrb) this.orbs.get(index)).cY;
                this.orbs.set(index, orbToSet);
                ((AbstractOrb) this.orbs.get(index)).setSlot(index, this.maxOrbs);
                orbToSet.playChannelSFX();
                for (AbstractPower p : this.powers)
                    p.onChannel(orbToSet);
                AbstractDungeon.actionManager.orbsChanneledThisCombat.add(orbToSet);
                AbstractDungeon.actionManager.orbsChanneledThisTurn.add(orbToSet);
            } else {
                AbstractDungeon.actionManager.addToTop(new ChannelAction(orbToSet));
                AbstractDungeon.actionManager.addToTop(new EvokeOrbAction(1));
                AbstractDungeon.actionManager.addToTop(new AnimateOrbAction(1));
            }
        }
    }

    public void increaseMaxOrbSlots(int amount, boolean playSfx) {
        if (this.maxOrbs >= 5) {
            AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0F, "槽位已满！", true));
        } else {
            if (playSfx)
                CardCrawlGame.sound.play("ORB_SLOT_GAIN", 0.1F);
            this.maxOrbs += amount;
            int i;
            for (i = 0; i < amount; i++) {
                this.orbs.add(new EmptyDiceSlot());
            }
            for (i = 0; i < this.orbs.size(); i++) {
                ((AbstractOrb) this.orbs.get(i)).setSlot(i, this.maxOrbs);
            }
        }
    }

    public void evokeOrb() {
        if (!this.orbs.isEmpty() && !(this.orbs.get(0) instanceof EmptyOrbSlot)) {
            ((AbstractOrb) this.orbs.get(0)).onEvoke();
            AbstractOrb orbSlot = new EmptyDiceSlot();

            int i;
            for (i = 1; i < this.orbs.size(); i++) {
                Collections.swap(this.orbs, i, i - 1);
            }

            this.orbs.set(this.orbs.size() - 1, orbSlot);

            for (i = 0; i < this.orbs.size(); i++) {
                ((AbstractOrb) this.orbs.get(i)).setSlot(i, this.maxOrbs);
            }
        }
    }

    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        return new AbstractGameAction.AttackEffect[] { AbstractGameAction.AttackEffect.SLASH_HEAVY,
                AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.SLASH_DIAGONAL,
                AbstractGameAction.AttackEffect.SLASH_HEAVY,
                AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.SLASH_DIAGONAL };
    }

    public ArrayList<AbstractCard> getCardPool(ArrayList<AbstractCard> tmpPool) {
        ArrayList<AbstractCard> poolCards = new ArrayList<>();
        ArrayList<AbstractCard> allowedCards = new ArrayList<>();
        for (AbstractPackage p : modcore.validPackage) {
            System.out.println(p.ID);
            if (p.ID.equals("Double:NullPackage")) {
                continue;
            }
            allowedCards.addAll(p.CardLists);
        }
        for (AbstractCard card : CardLibrary.getAllCards()) {
            // 自己的颜色（基类用 this.getCardColor()，SS 即 SS_Yellow，变种用各自颜色）：
            // 只放入 Default 卡包的牌，避免同色卡包牌重复
            if (card.color == this.getCardColor()) {
                if (card instanceof AbstractDoubleCard) {
                    AbstractDoubleCard d = (AbstractDoubleCard) card;
                    if (d.packagetype == PackageEnum.Default) {
                        allowedCards.add(card.makeStatEquivalentCopy());
                    }
                }
                continue;
            }
            if (card.color == AbstractDungeon.player.getCardColor()) {
                allowedCards.add(card.makeStatEquivalentCopy());
            }
        }
        System.out.println("allowedCards:");
        for (AbstractCard c : allowedCards) {
            System.out.println(c);
            poolCards.add(c);
            switch (c.rarity) {
                case COMMON:
                    AbstractDungeon.commonCardPool.addToTop(c);
                    break;
                case UNCOMMON:
                    AbstractDungeon.uncommonCardPool.addToTop(c);
                    break;
                case RARE:
                    AbstractDungeon.rareCardPool.addToTop(c);
                    break;
            }
        }
        return poolCards;
    }
}
