package SS.monster.ally;

import java.util.ArrayList;

import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.green.Backflip;
import com.megacrit.cardcrawl.cards.red.Defend_Red;
import com.megacrit.cardcrawl.cards.red.Strike_Red;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.MonsterStrings;

import SS.helper.ModHelper;

public class SoulAlly extends AbstractAlly {
    public static final String ID = ModHelper.makePath("SoulAlly");
    private static final MonsterStrings MONSTER_STRINGS = CardCrawlGame.languagePack.getMonsterStrings(ID);
    public static final String NAME = MONSTER_STRINGS.NAME;
    private static final String IMG_PATH = "img/monster/ally/SoulAlly.png";

    private static final int MAX_HP = 3;
    private static final int DAMAGE_AMOUNT = 3;

    public SoulAlly(float offsetX, float offsetY) {
        // 注意最后一个参数 60.0F
        // 这意味着 Hitbox 会比脚底板高出 60 像素
        // 这样格挡碎裂就会在半空中，而不是脚底
        super(NAME, ID, 20, IMG_PATH, TauntType.OVERFLOW,
                offsetX, offsetY - 50.0F * Settings.scale, 0F, -10F, 150F, 250F,
                3, 60.0F);

        // UI 微调
        this.energyOffsetX = 10.0F;
        this.energyOffsetY = 30.0F;

        this.cardScale = 0.25F;
        this.hoverScale = 0.75F;

        this.handOffsetX = 0.0F;
        this.handOffsetY = -80.0F;

        init();
    }

    // 【唯一需要你写逻辑的地方】：定义这个友军带什么牌
    @Override
    protected ArrayList<AbstractCard> getInitialDeck() {
        ArrayList<AbstractCard> list = new ArrayList<>();
        // list.add(new Inflame()); // 燃烧
        list.add(new Strike_Red());
        list.add(new Defend_Red());
        list.add(new Backflip());
        list.add(new Strike_Red());
        list.add(new Defend_Red());
        return list;
    }

    public void updateIntent(int num) {
    }

    @Override
    public void getMove(int num) {
        this.updateIntent(num);
    }

    @Override
    public void takeTurn() {
        // switch (this.nextMove) {
        // case 0:
        // addToBot(new DamageAction(AbstractDungeon.player, this.damage.get(0),
        // AbstractGameAction.AttackEffect.SLASH_HEAVY));
        // break;
        // }
        // 要加一个rollmove的action，重roll怪物的意图
        addToBot(new RollMoveAction(this));
    }
}