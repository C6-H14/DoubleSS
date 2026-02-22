package SS.action.monster;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import SS.monster.AbstractCardMonster;

public class AllyShuffleAction extends AbstractGameAction {
    private AbstractCardMonster owner;

    public AllyShuffleAction(AbstractCardMonster owner) {
        this.owner = owner;
    }

    @Override
    public void update() {
        if (owner != null) {
            // 播放洗牌音效
            com.megacrit.cardcrawl.core.CardCrawlGame.sound.play("SHUFFLE", 0.1F);
            // 执行洗牌逻辑 (确定性排序)
            owner.reshuffleDiscardPile();
        }
        this.isDone = true;
    }
}