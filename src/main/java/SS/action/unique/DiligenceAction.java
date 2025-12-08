package SS.action.unique;

import java.util.ArrayList;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class DiligenceAction extends AbstractGameAction {
    private DamageInfo info;

    public DiligenceAction(AbstractCreature target, int amount, DamageInfo info) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = ActionType.DAMAGE;
        this.duration = Settings.ACTION_DUR_MED;
        this.amount = amount;
    }

    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED && this.target != null) {
            AbstractDungeon.effectList
                    .add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, AttackEffect.NONE));
            this.target.damage(this.info);
            if ((((AbstractMonster) this.target).isDying || this.target.currentHealth <= 0) && !this.target.halfDead
                    && !this.target.hasPower("Minion")) {
                int cnt = 0;
                for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                    if (c.canUpgrade()) {
                        ++cnt;
                    }
                }
                int times = Math.max(1, cnt / this.amount);
                System.out.println("cnt:" + cnt + " times:" + times + " amount:" + this.amount);
                if (cnt == 0)
                    times = 0;
                for (int i = 0; i < times; i++) {
                    ArrayList<AbstractCard> possibleCards = new ArrayList<>();
                    for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                        if (c.canUpgrade()) {
                            possibleCards.add(c);
                        }
                    }

                    if (!possibleCards.isEmpty()) {
                        AbstractCard card = possibleCards
                                .get(AbstractDungeon.miscRng.random(0, possibleCards.size() - 1));
                        card.upgrade();
                        AbstractDungeon.player.bottledCardUpgradeCheck(card);
                        card.upgrade();
                        float x = MathUtils.random(0.1F, 0.9F) * Settings.WIDTH;
                        float y = MathUtils.random(0.2F, 0.8F) * Settings.HEIGHT;
                        AbstractDungeon.effectList.add(new ShowCardBrieflyEffect(card.makeStatEquivalentCopy(), x, y));
                        AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect(x, y));
                    }
                }
            }

            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
        }

        this.tickDuration();

    }
}