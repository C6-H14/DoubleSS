package SS.relic;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.powers.BerserkPower;
import com.megacrit.cardcrawl.powers.DrawPower;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import SS.helper.ModHelper;
import basemod.abstracts.CustomRelic;
import basemod.abstracts.CustomSavable;

public class Merit extends CustomRelic implements CustomSavable<Map<String, Integer>> {
    public static final String ID = ModHelper.makePath("Merit");
    private static final String IMG_PATH = "img/relic/Merit.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public int strengthCounter;
    public int dexterityCounter;
    public int focusCounter;

    public Merit() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + this.DESCRIPTIONS[1];
    }

    public void addMerit(int num) {
        this.flash();
        switch (num) {
            case 1: {
                ++this.strengthCounter;
                break;
            }
            case 2: {
                ++this.dexterityCounter;
                break;
            }
            case 3: {
                ++this.focusCounter;
                break;
            }
        }
        this.checkMaxCounter();
        this.checkMaxCounter();
        if (this.strengthCounter > 6) {
            this.strengthCounter = 6;
        }
        updateDescription(null);
    }

    private void checkMaxCounter() {
        if (this.strengthCounter > 4) {
            this.strengthCounter = 4;
        }
        if (this.dexterityCounter > 3) {
            this.dexterityCounter = 3;
        }
        if (this.focusCounter > 2) {
            this.focusCounter = 2;
        }
    }

    public void atBattleStart() {
        final AbstractPlayer p = AbstractDungeon.player;
        if (this.strengthCounter > 0 || this.dexterityCounter > 0 || this.focusCounter > 0) {
            this.flash();
            this.addToBot((AbstractGameAction) new RelicAboveCreatureAction(p, this));
        }
        if (this.strengthCounter > 0) {
            this.addToBot(new DamageAllEnemiesAction(p, DamageInfo.createDamageMatrix(5 * this.strengthCounter, true),
                    DamageType.THORNS, AbstractGameAction.AttackEffect.BLUNT_LIGHT));
        }
        if (this.dexterityCounter > 0) {
            this.addToBot(new ApplyPowerAction(p, p, new DrawPower(p, this.dexterityCounter)));
            this.addToBot(new ApplyPowerAction(p, p, new MetallicizePower(p, 3 * this.dexterityCounter)));
        }
        if (this.focusCounter > 0) {
            addToBot(new ApplyPowerAction(p, p, new BerserkPower(p, this.focusCounter)));
        }
    }

    public void renderCounter(final SpriteBatch sb, final boolean inTopPanel) {
        float offsetX = 0.0f;
        try {
            final Field field = AbstractRelic.class.getDeclaredField("offsetX");
            field.setAccessible(true);
            offsetX = field.getFloat(null);
        } catch (NoSuchFieldException | IllegalAccessException ex2) {
            final ReflectiveOperationException ex = null;
            final ReflectiveOperationException e = ex;
            e.printStackTrace();
        }
        if (this.strengthCounter > -1) {
            if (inTopPanel) {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont,
                        Integer.toString(this.strengthCounter), offsetX + this.currentX + 30.0f * Settings.scale,
                        this.currentY - 7.0f * Settings.scale, Color.RED);
            } else {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont,
                        Integer.toString(this.strengthCounter), this.currentX + 30.0f * Settings.scale,
                        this.currentY - 7.0f * Settings.scale, Color.RED);
            }
        }
        if (this.dexterityCounter > -1) {
            if (inTopPanel) {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont,
                        Integer.toString(this.dexterityCounter), offsetX + this.currentX + 30.0f * Settings.scale,
                        this.currentY + 18.0f * Settings.scale, Color.GREEN);
            } else {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont,
                        Integer.toString(this.dexterityCounter), this.currentX + 30.0f * Settings.scale,
                        this.currentY + 18.0f * Settings.scale, Color.GREEN);
            }
        }
        if (this.focusCounter > -1) {
            if (inTopPanel) {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont,
                        Integer.toString(this.focusCounter), offsetX + this.currentX + 30.0f * Settings.scale,
                        this.currentY + 43.0f * Settings.scale, Color.BLUE);
            } else {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont,
                        Integer.toString(this.focusCounter), this.currentX + 30.0f * Settings.scale,
                        this.currentY + 43.0f * Settings.scale, Color.BLUE);
            }
        }
    }

    public Map<String, Integer> onSave() {
        final Map<String, Integer> ret = new HashMap<String, Integer>();
        ret.put("strengthCounter", this.strengthCounter);
        ret.put("dexterityCounter", this.dexterityCounter);
        ret.put("focusCounter", this.focusCounter);
        return ret;
    }

    public void onLoad(final Map<String, Integer> maps) {
        if (maps != null) {
            this.strengthCounter = maps.getOrDefault("strengthCounter", 0);
            this.dexterityCounter = maps.getOrDefault("dexterityCounter", 0);
            this.focusCounter = maps.getOrDefault("focusCounter", 0);
        }
    }
}
