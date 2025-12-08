package SS.power;

import SS.helper.ModHelper;
import SS.relic.Merit;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.EscapeAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.RelicAboveCreatureEffect;

public class CharityPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("CharityPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public CharityPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/CharityPower84.png";
        String path48 = "img/power/CharityPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atEndOfTurn(final boolean isPlayer) {
        if (!isPlayer)
            return;
        int cnt = this.owner.gold;
        this.owner.loseGold(15);
        if (cnt > 15)
            cnt = 15;
        System.out.println("cnt:" + cnt);
        for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
            addToBot(new ApplyPowerAction(m, owner, new FundsPower(m, cnt * this.amount)));
            if (cnt * this.amount >= m.currentHealth && m instanceof AbstractMonster) {
                int num = 0;
                if (!m.hasPower("Minion"))
                    num = 1;
                if ((AbstractDungeon.getCurrRoom()).eliteTrigger)
                    num = 2;
                for (AbstractMonster mm : (AbstractDungeon.getMonsters()).monsters) {
                    if (mm.type == AbstractMonster.EnemyType.BOSS) {
                        num = 3;
                    }
                }
                addToTop((AbstractGameAction) new EscapeAction((AbstractMonster) m));
                final AbstractRelic meritRelic = AbstractDungeon.player.getRelic(Merit.ID);
                if (meritRelic != null) {
                    final AbstractRelic abstractRelic = meritRelic;
                    if (abstractRelic instanceof Merit) {
                        ((Merit) abstractRelic).addMerit(num);
                    }
                    AbstractDungeon.effectList.add(new RelicAboveCreatureEffect(AbstractDungeon.player.hb.cX,
                            AbstractDungeon.player.hb.cY, meritRelic));
                } else {
                    final Merit newMeritRelic = new Merit();
                    if (newMeritRelic instanceof Merit) {
                        ((Merit) newMeritRelic).addMerit(num);
                    }
                    AbstractDungeon.getCurrRoom().spawnRelicAndObtain((float) (Settings.WIDTH / 2),
                            (float) (Settings.HEIGHT / 2), newMeritRelic);
                    AbstractDungeon.effectList.add(new RelicAboveCreatureEffect(AbstractDungeon.player.hb.cX,
                            AbstractDungeon.player.hb.cY, newMeritRelic));
                }
            }
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0];
    }
}