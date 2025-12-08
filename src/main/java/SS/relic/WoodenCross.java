package SS.relic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import SS.helper.ModHelper;
import SS.power.DyingPower;
import basemod.abstracts.CustomRelic;

public class WoodenCross extends CustomRelic {
    public static final String ID = ModHelper.makePath("WoodenCross");
    private static final RelicStrings RELIC_STRINGS = CardCrawlGame.languagePack.getRelicStrings(ID);
    private static final String IMG_PATH = "img/relic/WoodenCross.png";
    private static final AbstractRelic.RelicTier RELIC_TIER = AbstractRelic.RelicTier.SPECIAL;
    private static final AbstractRelic.LandingSound LANDING_SOUND = AbstractRelic.LandingSound.FLAT;
    public static final String DESCRIPTION[] = RELIC_STRINGS.DESCRIPTIONS;
    private int amount = 3;

    public WoodenCross() {
        super(ID, new Texture(Gdx.files.internal(IMG_PATH)), RELIC_TIER, LANDING_SOUND);
        this.amount = 3;
    }

    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    private int countAmount() {
        int cnt = this.amount;
        long hp = 0;
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            hp += m.currentHealth;
        }
        cnt = cnt + (int) (hp / 60);
        return cnt;
    }

    public void atPreBattle() {
        flash();
        addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player,
                new DyingPower(AbstractDungeon.player, countAmount()), countAmount()));
        this.grayscale = true;
    }

    public void atTurnStart() {
        int damage = 0;
        if (AbstractDungeon.player.hasPower("Double:DyingPower")) {
            damage = 3 * AbstractDungeon.player.getPower("Double:DyingPower").amount;
        }
        addToBot(new DamageAllEnemiesAction(null, DamageInfo.createDamageMatrix(damage, true),
                DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.BLUNT_LIGHT));
    }

    public void justEnteredRoom(AbstractRoom room) {
        this.grayscale = false;
    }

    public AbstractRelic makeCopy() {
        return new WoodenCross();
    }
}
