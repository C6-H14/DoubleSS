package SS.action.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.screens.DeathScreen;

public class DieAction extends AbstractGameAction {
    private boolean HitAll;
    private AbstractMonster m;

    public DieAction() {
    }

    public void update() {
        AbstractDungeon.player.isDead = true;
        AbstractDungeon.deathScreen = new DeathScreen(AbstractDungeon.getMonsters());
        this.isDone = true;
    }
}