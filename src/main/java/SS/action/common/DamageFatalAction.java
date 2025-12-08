package SS.action.common;

import java.util.function.Consumer;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

import SS.modcore.modcore;

public class DamageFatalAction extends AbstractGameAction {// 用于斩杀敌人时触发
    private DamageInfo info;
    private static final float DURATION = 0.1F;
    Consumer<DamageInfo> c;

    public DamageFatalAction(AbstractCreature target, DamageInfo info, Consumer<DamageInfo> c) {
        this.info = info;
        this.setValues(target, info);
        this.c = c;
        this.actionType = ActionType.DAMAGE;
        this.duration = 0.1F;
    }

    public void update() {
        if (this.duration == 0.1F && this.target != null) {
            AbstractDungeon.effectList
                    .add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, AttackEffect.NONE));
            this.target.damage(this.info);
            if ((((AbstractMonster) this.target).isDying || this.target.currentHealth <= 0) && !this.target.halfDead
                    && !this.target.hasPower("Minion") && modcore.combatReward.rewards.isEmpty()) {
                c.accept(info);
            }

            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
        }

        this.tickDuration();
    }
}