package SS.Dice;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard.CardTags;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.vfx.BobEffect;

import SS.effect.DicePassiveEffect;
import SS.effect.DiceTriggeredEffect;

import java.util.ArrayList;

import com.megacrit.cardcrawl.core.AbstractCreature;

public abstract class AbstractDice extends AbstractOrb {
    protected float vfxTimer = 0.3F;
    public int forterate = 1;
    protected Color myColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    public int faces = 6;
    public int result = -1;
    public AbstractCreature target;
    public ArrayList<CardTags> tags = new ArrayList<>();

    public AbstractDice() {
        this.c = Settings.CREAM_COLOR.cpy();
        this.shineColor = new Color(1.0F, 1.0F, 1.0F, 0.0F);
        this.hb = new Hitbox(96.0F * Settings.scale, 96.0F * Settings.scale);
        this.img = null;
        this.bobEffect = new BobEffect(3.0F * Settings.scale, 3.0F);
        this.fontScale = 0.7F;
        this.channelAnimTimer = 0.5F;
        this.basePassiveAmount = 0;
        this.faces = 6;
        this.passiveAmount = this.result = getDiceResult();
    }

    public AbstractDice(int f) {
        this.c = Settings.CREAM_COLOR.cpy();
        this.shineColor = new Color(1.0F, 1.0F, 1.0F, 0.0F);
        this.hb = new Hitbox(96.0F * Settings.scale, 96.0F * Settings.scale);
        this.img = null;
        this.bobEffect = new BobEffect(3.0F * Settings.scale, 3.0F);
        this.fontScale = 0.7F;
        this.channelAnimTimer = 0.5F;
        this.basePassiveAmount = 0;
        this.faces = f;
        this.passiveAmount = this.result = getDiceResult();
    }

    public void upgradeFaces(int amount) {
        this.faces = amount;
        this.result = getDiceResult();
    }

    protected void renderText(SpriteBatch sb) {
        if (!(this instanceof EmptyDiceSlot)) {
            FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L,
                    Integer.toString(this.passiveAmount), this.cX + NUM_X_OFFSET,
                    this.cY + this.bobEffect.y / 2.0F + NUM_Y_OFFSET - 4.0F * Settings.scale,
                    new Color(0.2F, 1.0F, 1.0F, this.c.a), this.fontScale);
            FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L,
                    Integer.toString(this.evokeAmount), this.cX + NUM_X_OFFSET,
                    this.cY + this.bobEffect.y / 2.0F + NUM_Y_OFFSET + 20.0F * Settings.scale, this.c, this.fontScale);
        }
    }

    public int getDiceResult(boolean useCardRng)// 骰子随机数判定
    {
        int amount = 1;
        if (AbstractDungeon.player.hasPower("Double:RerollPower")) {
            amount += AbstractDungeon.player.getPower("Double:RerollPower").amount;
        }
        if (useCardRng) {
            return Math.min(faces, amount + AbstractDungeon.cardRandomRng.random(faces - 1) + 1);
        } else {
            return Math.min(faces, MathUtils.random(faces - 1) + amount);
        }
    }

    public int getDiceResult() {
        return getDiceResult(true);
    }

    public void onEvoke()// 激发骰子
    {
        if (this instanceof EmptyDiceSlot)
            return;
        myEvoke();
    }

    public void onEndOfTurn() {
        // triggerEvokeAnimation();
        // onEvoke();
    }

    public void playChannelSFX() {
        CardCrawlGame.sound.play("ORB_LIGHTNING_EVOKE", 0.2F);
    }

    public void updateAnimation() {
        super.updateAnimation();
        updateDescription();
        this.angle += Gdx.graphics.getDeltaTime() * 2.0F;
        this.vfxTimer -= Gdx.graphics.getDeltaTime();
        if (!(this instanceof EmptyDiceSlot) && this.vfxTimer < 0.0F) {

            AbstractDungeon.effectList.add(new DicePassiveEffect(this.cX, this.cY, this.myColor.cpy()));
            if (MathUtils.randomBoolean()) {
                AbstractDungeon.effectList.add(new DicePassiveEffect(this.cX, this.cY, this.myColor.cpy()));
            }
            this.vfxTimer = MathUtils.random(0.2F, 0.6F);
        }
    }

    public void render(SpriteBatch sb) {
        float scale = 1.0F + MathUtils.sin(this.angle) * 0.05F + 0.05F;
        this.shineColor = this.c.cpy();
        this.c.a /= 2.0F * scale - 1.0F;
        sb.setBlendFunction(770, 771);
        sb.setColor(this.shineColor);
        sb.draw(this.img, this.cX - 48.0F, this.cY - 48.0F + this.bobEffect.y * 0.5F, 48.0F, 48.0F, 96.0F, 96.0F,
                this.scale * scale, this.scale * scale, 0.0F, 0, 0, 96, 96, false, false);

        if (AbstractDungeon.player.hasPower("MGR:HarmonyForm") && StartOrEnd()) {

            Color tmpColor = this.shineColor.cpy();
            tmpColor.a /= 2.0F;
            sb.setColor(tmpColor);
            sb.setBlendFunction(770, 1);
            sb.draw(this.img, this.cX - 48.0F, this.cY - 48.0F + this.bobEffect.y * 0.5F, 48.0F, 48.0F, 96.0F, 96.0F,
                    this.scale * scale * 1.3F, this.scale * scale * 1.3F, 0.0F, 0, 0, 96, 96, false, false);

            sb.setBlendFunction(770, 771);
        }
        renderText(sb);
        this.hb.render(sb);
    }

    public static ArrayList<AbstractDice> getAllDice(int evokeAmount, AbstractPlayer p, AbstractMonster m) {
        ArrayList<AbstractDice> orbs = new ArrayList();
        orbs.add(new AttackDice(evokeAmount, m));
        orbs.add(new AttackHaoDice(evokeAmount, m));
        orbs.add(new DefendDice(evokeAmount, p));
        orbs.add(new DefendHaoDice(evokeAmount, p));
        orbs.add(new EternalAttackDice(evokeAmount, p));
        orbs.add(new EternalDefendDice(evokeAmount, p));
        orbs.add(new ImmolateDice(evokeAmount, m));
        orbs.add(new IronwaveDice(evokeAmount, m));
        orbs.add(new PeptideDice(evokeAmount, m));
        orbs.add(new WitherDice(evokeAmount, m));
        return orbs;
    }

    public void triggerEvokeAnimation() {
        AbstractDungeon.effectsQueue
                .add(new DiceTriggeredEffect(this.cX - 48.0F, this.cY - 48.0F, this.img, this.scale, this.c.cpy()));
    }

    public boolean StartOrEnd() {
        AbstractPlayer p = AbstractDungeon.player;
        return (equals(p.orbs.get(0)) || equals(p.orbs.get(p.orbs.size() - 1)));
    }

    public abstract void myEvoke();

    public int getDiceEvokeAmount() {
        return this.baseEvokeAmount;
    }

    public void updateEvokeAmount(int amount) {
        this.baseEvokeAmount = this.evokeAmount = amount;
    }

    public static AbstractDice getRandomDice(int evokeAmount, AbstractMonster m) {
        return getRandomDice(true, evokeAmount, AbstractDungeon.player, m);
    }

    public static AbstractDice getRandomDice(int evokeAmount, AbstractMonster m, CardTags tag) {
        return getRandomDice(true, evokeAmount, AbstractDungeon.player, m, tag);
    }

    public static AbstractDice getRandomDice(int evokeAmount, AbstractPlayer p, AbstractMonster m) {
        return getRandomDice(true, evokeAmount, p, m);
    }

    public static AbstractDice getRandomDice(boolean useCardRng, int evokeAmount, AbstractPlayer p, AbstractMonster m,
            SS.modcore.modcore.CardTags tag) {
        return getRandomDice(true, evokeAmount, p, m, tag);
    }

    public static AbstractDice getRandomDice(boolean useCardRng, int evokeAmount, AbstractPlayer p, AbstractMonster m) {
        ArrayList<AbstractDice> orbs = getAllDice(evokeAmount, p, m);
        return useCardRng ? orbs.get(AbstractDungeon.cardRandomRng.random(orbs.size() - 1))
                : orbs.get(MathUtils.random(orbs.size() - 1));
    }

    public static AbstractDice getRandomDice(boolean useCardRng, int evokeAmount, AbstractPlayer p, AbstractMonster m,
            CardTags tag) {
        ArrayList<AbstractDice> orbs = getAllDice(evokeAmount, p, m);
        if (tag != null) {
            for (AbstractDice d : orbs) {
                if (!d.tags.contains(tag)) {
                    orbs.remove(d);
                }
            }
        }
        return useCardRng ? orbs.get(AbstractDungeon.cardRandomRng.random(orbs.size() - 1))
                : orbs.get(MathUtils.random(orbs.size() - 1));
    }

    public abstract AbstractDice makeCopy();
}