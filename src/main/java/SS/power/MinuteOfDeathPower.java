package SS.power;

import SS.helper.ModHelper;
import basemod.BaseMod;
import basemod.interfaces.PostUpdateSubscriber;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.cards.DamageInfo;

public class MinuteOfDeathPower extends AbstractPower implements PostUpdateSubscriber {
    public static final String POWER_ID = ModHelper.makePath("MinuteOfDeathPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    private float duration;

    // 订阅状态锁，防止内存泄漏和并发修改异常
    private boolean isSubscribed = false;

    public MinuteOfDeathPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;
        this.duration = amount;
        this.isTurnBased = false;

        loadRegion("time");
        updateDescription();
        BaseMod.subscribe(this);
        this.isSubscribed = true;
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void receivePostUpdate() {
        // 1. 如果已经取消订阅，直接返回
        if (!isSubscribed) {
            return;
        }

        // 2. 【铁壁防御】检查游戏是否处于运行状态
        // 如果玩家返回了主菜单、放弃了游戏，或者地下城节点还没生成，直接自我销毁
        if (com.megacrit.cardcrawl.core.CardCrawlGame.dungeon == null ||
                !com.megacrit.cardcrawl.core.CardCrawlGame.isInARun() ||
                AbstractDungeon.currMapNode == null) {
            safeUnsubscribe();
            return;
        }

        // 3. 安全获取当前房间
        com.megacrit.cardcrawl.rooms.AbstractRoom room = AbstractDungeon.getCurrRoom();

        // 4. 如果不在战斗中，暂停计时 (并且如果 owner 已经不在场了，也可以考虑销毁)
        if (room == null || room.phase != com.megacrit.cardcrawl.rooms.AbstractRoom.RoomPhase.COMBAT) {
            return;
        }

        // 5. 拥有者死亡防漏判定
        if (this.owner == null || this.owner.isDeadOrEscaped()) {
            safeUnsubscribe();
            return;
        }

        // ----------------- 下面是原本的计时逻辑 -----------------
        if (this.duration > 0.0f) {
            this.duration -= Gdx.graphics.getDeltaTime();
            int secondsLeft = (int) Math.ceil(this.duration);

            if (secondsLeft != this.amount) {
                this.amount = secondsLeft;
                this.updateDescription();

                // 时间到，执行死刑
                if (this.amount <= 0) {
                    this.amount = 0;
                    this.duration = 0.0f;
                    safeUnsubscribe(); // 处刑前先解除订阅，防止后续报错
                    executeDeath();
                }
            }
        }
    }

    private void executeDeath() {
        // 1. 如果玩家还在选牌/看其他界面，强行关闭它！(极具压迫感)
        if (AbstractDungeon.isScreenUp) {
            AbstractDungeon.closeCurrentScreen();
        }

        // 2. 【关键】直接调用底层 damage() 方法，而不是放入 Action 队列。
        // 因为在刚关掉 UI 的瞬间，ActionManager 可能还没恢复。
        // 直接扣血是最稳妥的“真实死亡”。
        this.owner.damage(new DamageInfo(this.owner, 99999, DamageInfo.DamageType.HP_LOSS));
    }

    @Override
    public void stackPower(int stackAmount) {
        // 再次获得该能力时，重置为 60 秒
        this.amount = 60;
        this.duration = 60.0f;
        this.updateDescription();
    }

    // =================================================================
    // 生命周期管理 (极其重要，防止跳出战斗后还在后台跑报错)
    // =================================================================
    private void safeUnsubscribe() {
        if (isSubscribed) {
            this.isSubscribed = false;
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                basemod.BaseMod.unsubscribe(this, basemod.interfaces.PostUpdateSubscriber.class);
            });
        }
    }

    // 当能力被清除（比如用到人工制品、或者死亡）
    @Override
    public void onRemove() {
        safeUnsubscribe();
    }

    // 当战斗胜利
    @Override
    public void onVictory() {
        safeUnsubscribe();
    }

    // 当拥有者死亡
    @Override
    public void onDeath() {
        safeUnsubscribe();
    }
}
