package SS.monster.ally;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import SS.helper.ModHelper;

public class SoulAlly extends AbstractAlly {
    // 1. 定义基本信息
    public static final String ID = ModHelper.makePath("SoulAlly");
    private static final MonsterStrings MONSTER_STRINGS = CardCrawlGame.languagePack.getMonsterStrings(ID);
    public static final String NAME = MONSTER_STRINGS.NAME;
    private static final String IMG_PATH = "img/monster/ally/SoulAlly.png";

    private static final int MAX_HP = 3;
    private static final int DAMAGE_AMOUNT = 3;

    public SoulAlly() {
        super(NAME, ID, 3, IMG_PATH, TauntType.OVERFLOW, 29F, 58F);
        init();
    }

    // 构造函数 2: 指定相对位置
    public SoulAlly(float offsetX, float offsetY) {
        // 调用新的父类构造函数
        super(NAME, ID, 3, IMG_PATH, TauntType.OVERFLOW, offsetX, offsetY, 29F, 58F);
        init();
    }

    // 提取公共初始化意图逻辑
    public void init() {
        this.updateIntent(0);
        this.createIntent();
    }

    @Override
    public void updateIntent(int num) {
        // 标记为单体
        this.isAOE = false;

        this.setMove((byte) 1, Intent.ATTACK, 3);
    }

    @Override
    public void atEndOfTurn() {
        // 获取锁定的目标 (不要再调用 getRandomTarget 了，用 getTarget)
        AbstractMonster target = this.getTarget();

        if (target != null) {
            this.attack(target, 3, AbstractGameAction.AttackEffect.FIRE);
        }

        // 记得调用 super 清理锁定
        super.atEndOfTurn();
    }
}
