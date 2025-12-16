package SS.characters;

import basemod.abstracts.CustomPlayer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.defect.AnimateOrbAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.defect.EvokeOrbAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.cutscenes.CutscenePanel;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;

import SS.Dice.AbstractDice;
import SS.Dice.EmptyDiceSlot;
import SS.cards.MultiFacial;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.path.AbstractCardEnum;
import SS.path.ThmodClassEnum;

import java.util.ArrayList;
import java.util.Collections;

public class MyCharacter extends CustomPlayer {
    private static final String MY_CHARACTER_SHOULDER_1 = "img/char/shoulder.png";
    private static final String MY_CHARACTER_SHOULDER_2 = "img/char/shoulder2.png";
    private static final String CORPSE_IMAGE = "img/char/Corpse.png";
    private static final String[] ORB_TEXTURES = new String[] { "img/UI/orb/layer5.png", "img/UI/orb/layer4.png",
            "img/UI/orb/layer3.png", "img/UI/orb/layer2.png", "img/UI/orb/layer1.png", "img/UI/orb/layer6.png",
            "img/UI/orb/layer5d.png", "img/UI/orb/layer4d.png", "img/UI/orb/layer3d.png", "img/UI/orb/layer2d.png",
            "img/UI/orb/layer1d.png" };
    private static final float[] LAYER_SPEED = new float[] { -40.0F, -32.0F, 20.0F, -20.0F, 0.0F, -10.0F, -8.0F, 5.0F,
            -5.0F, 0.0F };
    private static final CharacterStrings characterStrings = CardCrawlGame.languagePack
            .getCharacterString("Double:Double");
    public static final Color SS_Yellow = new Color(0.984375F, 0.91796875F, 0.16796875F, 1.0F);

    public static class PlayerColorEnum {
        @SpireEnum
        public static AbstractPlayer.PlayerClass SS;
        @SpireEnum
        public static AbstractCard.CardColor SS_Yellow;
        @SpireEnum
        public static AbstractCard.CardColor Hao_Green;
    }

    public static class PlayerLibraryEnum {
        @SpireEnum
        public static CardLibrary.LibraryType SS_Yellow;
        @SpireEnum
        public static CardLibrary.LibraryType Hao_Green;
        @SpireEnum
        public static CardLibrary.LibraryType Lost_Black;
        @SpireEnum
        public static CardLibrary.LibraryType Shock_Blue;
    }

    public MyCharacter(String name) {
        super(name, ThmodClassEnum.SS_CLASS, ORB_TEXTURES, "img/UI/orb/vfx.png", LAYER_SPEED, null, null);
        this.dialogX = this.drawX + 0.0F * Settings.scale;
        this.dialogY = this.drawY + 150.0F * Settings.scale;
        initializeClass("img/char/Character.png", "img/char/shoulder2.png", "img/char/shoulder.png",
                "img/char/Corpse.png",
                getLoadout(), 0.0F, 0.0F, 300.0F, 300.0F, new EnergyManager(3));
    }

    public ArrayList<String> getStartingDeck() {
        ArrayList<String> retVal = new ArrayList<>();
        int x;
        for (x = 0; x < 4; x++) {
            retVal.add("Double:Strike");
        }
        for (x = 0; x < 4; x++) {
            retVal.add("Double:Defend");
        }
        retVal.add("Double:RecordingTeam");
        retVal.add("Double:Seething");
        return retVal;
    }

    public ArrayList<String> getStartingRelics() {
        ArrayList<String> retVal = new ArrayList<>();
        retVal.add("Double:LCysteine");
        return retVal;
    }

    public CharSelectInfo getLoadout() {
        return new CharSelectInfo("二硫键", "二硫键看到这里记得发一下录播", 70, 70, 5, 99, 5, (AbstractPlayer) this,
                getStartingRelics(),
                getStartingDeck(), false);
    }

    public String getTitle(AbstractPlayer.PlayerClass playerClass) {
        return "二硫键";
    }

    public AbstractCard.CardColor getCardColor() {
        return AbstractCardEnum.SS_Yellow;
    }

    public AbstractCard getStartCardForEvent() {
        return new MultiFacial();
    }

    public Color getCardTrailColor() {
        return SS_Yellow;
    }

    public int getAscensionMaxHPLoss() {
        return 7;
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
        if (orbToSet instanceof com.megacrit.cardcrawl.orbs.EmptyOrbSlot || orbToSet instanceof EmptyDiceSlot)
            return;
        if (!(orbToSet instanceof AbstractDice)) {
            AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0F,
                    /* (CardCrawlGame.languagePack.getTutorialString("SS:exception")).TEXT[0] */"我不是故障机器人，不能生成充能球。",
                    true));
        } else if (this.maxOrbs <= 0) {
            AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0F, MSG[4], true));
        } else {
            int index = -1;
            for (int i = 0; i < this.orbs.size(); i++) {
                if (this.orbs.get(i) instanceof EmptyDiceSlot) {
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
                AbstractDungeon.actionManager.addToTop(new ChannelAction((AbstractOrb) orbToSet));
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

    public ArrayList<CutscenePanel> getCutscenePanels() {
        ArrayList<CutscenePanel> panels = new ArrayList<>();
        panels.add(new CutscenePanel("img/char/scene1.png", "ATTACK_MAGIC_FAST_1"));
        panels.add(new CutscenePanel("img/char/scene2.png"));
        panels.add(new CutscenePanel("img/char/scene3.png"));
        return panels;
    }

    public void evokeOrb() {
        if (!this.orbs.isEmpty() && !(this.orbs.get(0) instanceof EmptyOrbSlot)
                && !(this.orbs.get(0) instanceof EmptyDiceSlot)) {
            ((AbstractOrb) this.orbs.get(0)).onEvoke();
            AbstractOrb orbSlot = new EmptyDiceSlot();

            int i;
            for (i = 1; i < this.orbs.size(); ++i) {
                Collections.swap(this.orbs, i, i - 1);
            }

            this.orbs.set(this.orbs.size() - 1, orbSlot);

            for (i = 0; i < this.orbs.size(); ++i) {
                ((AbstractOrb) this.orbs.get(i)).setSlot(i, this.maxOrbs);
            }
        }

    }

    public String getCustomModeCharacterButtonSoundKey() {
        return "ATTACK_HEAVY";
    }

    public String getLocalizedCharacterName() {
        return "二硫键";
    }

    public AbstractPlayer newInstance() {
        return (AbstractPlayer) new MyCharacter(this.name);
    }

    public String getSpireHeartText() {
        return characterStrings.TEXT[1];
    }

    public Color getSlashAttackColor() {
        return SS_Yellow;
    }

    public String getVampireText() {
        return "在一条昏暗的街上，你遇见几个戴着兜帽的人在进行某种黑暗的仪式。当你靠近时，他们全都同时转身面对你，让你觉得十分诡异。 其中个子最高的一个微微一笑，露出了长长的尖牙，向你伸出了一只苍白而瘦长的手： NL ~“加入我们，小逼崽子。一起来感受高塔的温暖吧。”~";
    }

    public Color getCardRenderColor() {
        return SS_Yellow;
    }

    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        return new AbstractGameAction.AttackEffect[] { AbstractGameAction.AttackEffect.SLASH_HEAVY,
                AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.SLASH_DIAGONAL,
                AbstractGameAction.AttackEffect.SLASH_HEAVY, AbstractGameAction.AttackEffect.FIRE,
                AbstractGameAction.AttackEffect.SLASH_DIAGONAL };
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