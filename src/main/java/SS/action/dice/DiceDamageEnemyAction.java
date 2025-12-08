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

public class DiceDamageEnemyAction extends AbstractGameAction {
    private boolean HitAll;
    private AbstractMonster m;

    public DiceDamageEnemyAction(int amount, AbstractMonster m, boolean HitAll) {
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.HitAll = HitAll;
        this.source = (AbstractCreature) AbstractDungeon.player;
        this.amount = amount;
        this.m = m;
    }

    public void update() {
        if (!this.HitAll) {
            AbstractMonster abstractMonster = m;
            DamageInfo info = new DamageInfo(this.source, this.amount, DamageInfoEnum.DICE);
            if (abstractMonster != null && !abstractMonster.isDeadOrEscaped() && !abstractMonster.halfDead) {
                /*
                 * if (abstractMonster.getPower("BlockReturnPower") != null) {
                 * abstractMonster.getPower("BlockReturnPower").flash();
                 * addToTop(new GainBlockAction(AbstractDungeon.player,
                 * abstractMonster.getPower("BlockReturnPower").amount, Settings.FAST_MODE));
                 * }
                 */
                addToTop(new DamageAction((AbstractCreature) abstractMonster, info,
                        GetEffect(this.amount), true));
            } else {
                abstractMonster = AbstractDungeon.getRandomMonster();
                if (abstractMonster != null) {
                    /*
                     * if (abstractMonster.getPower("BlockReturnPower") != null) {
                     * abstractMonster.getPower("BlockReturnPower").flash();
                     * addToTop(new GainBlockAction(AbstractDungeon.player,
                     * abstractMonster.getPower("BlockReturnPower").amount, Settings.FAST_MODE));
                     * }
                     */
                    addToTop(new DamageAction((AbstractCreature) abstractMonster, info,
                            GetEffect(this.amount), true));
                }
            }
        } else {
            for (AbstractMonster m : (AbstractDungeon.getMonsters()).monsters) {
                if (!m.isDeadOrEscaped() && !m.halfDead) {
                    DamageInfo info = new DamageInfo(this.source, this.amount, DamageInfoEnum.DICE);
                    if (m.getPower("BlockReturnPower") != null) {
                        m.getPower("BlockReturnPower").flash();
                        addToTop(new GainBlockAction(AbstractDungeon.player,
                                m.getPower("BlockReturnPower").amount, Settings.FAST_MODE));
                    }
                }
            }
            addToTop(new DamageAllEnemiesAction(AbstractDungeon.player, this.amount, DamageInfo.DamageType.THORNS,
                    GetEffect(amount)));
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