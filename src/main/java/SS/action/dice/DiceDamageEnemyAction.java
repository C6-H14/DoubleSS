package SS.action.dice;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import SS.path.DamageInfoEnum;
import SS.patches.DamageAllEnemiesDiceSource;
import SS.patches.DamageInfoDiceSource;
import SS.stats.DiceAttribution;

public class DiceDamageEnemyAction extends AbstractGameAction {
    private boolean HitAll;
    private AbstractMonster m;
    /** 战斗统计：产生本 action 的骰子归属快照（结算时回读，可为 null = 无统计）。 */
    private DiceAttribution diceAttribution;

    public DiceDamageEnemyAction(int amount, AbstractMonster m, boolean HitAll) {
        this(amount, m, HitAll, null);
    }

    public DiceDamageEnemyAction(int amount, AbstractMonster m, boolean HitAll, DiceAttribution diceAttribution) {
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.HitAll = HitAll;
        this.source = (AbstractCreature) AbstractDungeon.player;
        this.amount = amount;
        this.m = m;
        this.diceAttribution = diceAttribution;
    }

    public void update() {
        if (!this.HitAll) {
            AbstractMonster abstractMonster = m;
            DamageInfo info = new DamageInfo(this.source, this.amount, DamageInfoEnum.DICE);
            if (this.diceAttribution != null) {
                DamageInfoDiceSource.diceRef.set(info, this.diceAttribution);
            }
            if (abstractMonster != null && !abstractMonster.isDeadOrEscaped() && !abstractMonster.halfDead) {
                addToTop(new DamageAction((AbstractCreature) abstractMonster, info,
                        GetEffect(this.amount), true));
            } else {
                abstractMonster = AbstractDungeon.getRandomMonster();
                if (abstractMonster != null) {
                    addToTop(new DamageAction((AbstractCreature) abstractMonster, info,
                            GetEffect(this.amount), true));
                }
            }
        } else {
            for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
                if (!m.isDeadOrEscaped() && !m.halfDead) {
                    if (m.getPower("BlockReturnPower") != null) {
                        m.getPower("BlockReturnPower").flash();
                        addToTop(new GainBlockAction(AbstractDungeon.player,
                                m.getPower("BlockReturnPower").amount, Settings.FAST_MODE));
                    }
                }
            }
            DamageAllEnemiesAction aoe = new DamageAllEnemiesAction(AbstractDungeon.player, this.amount,
                    DamageInfo.DamageType.THORNS, GetEffect(amount));
            if (this.diceAttribution != null) {
                DamageAllEnemiesDiceSource.diceRef.set(aoe, this.diceAttribution);
            }
            addToTop(aoe);
        }
        this.isDone = true;
    }

    private AbstractGameAction.AttackEffect GetEffect(int dmg) {
        if (dmg <= 2)
            return AbstractGameAction.AttackEffect.NONE;
        if (dmg <= 6)
            return AbstractGameAction.AttackEffect.BLUNT_LIGHT;
        return AbstractGameAction.AttackEffect.BLUNT_HEAVY;
    }
}
