package SS.helper;

import java.util.ArrayList;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.vfx.cardManip.ExhaustCardEffect;
import com.megacrit.cardcrawl.localization.CardStrings;

import SS.interfaces.IEnvironmentCard;

public class EnvironmentManager {
    public static EnvironmentManager inst = new EnvironmentManager();

    // =================================================================
    // ⚙️ 可调参数
    // =================================================================
    public static float FLY_IN_DURATION = 0.6f;
    public static float FLY_OUT_DURATION = 0.4f;
    public static float TRANSITION_DELAY = 0.3f;
    public static float CARD_SCALE = 0.35f;
    public static float HOVER_SCALE = 0.65f;

    public static float TARGET_X = Settings.WIDTH / 2.0F;
    public static float TARGET_Y = Settings.HEIGHT * 0.8F;
    public static float SPACING = 160.0F * Settings.scale;
    // =================================================================

    public IEnvironmentCard activeCard = null;
    private IEnvironmentCard pendingCard = null;
    public ArrayList<AbstractCard> activeSubCards = new ArrayList<>();

    public enum State {
        IDLE, EXITING_OLD, WAITING, ENTERING_NEW, ACTIVE
    }

    public State state = State.IDLE;

    private float stateTimer = 0.0f;
    private float startX, startY;

    public static void playEnvironmentCard(IEnvironmentCard card) {
        inst.queueNewEnvironment(card);
    }

    private void queueNewEnvironment(IEnvironmentCard card) {
        this.pendingCard = card;
        this.stateTimer = 0.0f;

        if (this.activeCard != null) {
            this.state = State.EXITING_OLD;
            this.activeCard.onExitEnvironment();

            for (AbstractCard c : activeSubCards) {
                AbstractDungeon.effectsQueue.add(new ExhaustCardEffect(c));
            }
        } else {
            this.state = State.ENTERING_NEW;
            initFlyInCoordinates(card.asCard());
        }
    }

    private void initFlyInCoordinates(AbstractCard card) {
        this.startX = card.current_x;
        this.startY = card.current_y;
        createSubCards(card);
    }

    private void createSubCards(AbstractCard original) {
        activeSubCards.clear();
        String[] suffixes = { " I", " II", " III" };

        for (int i = 0; i < 3; i++) {
            AbstractCard sub = original.makeStatEquivalentCopy();

            sub.name = original.name + suffixes[i];
            sub.current_x = startX;
            sub.current_y = startY;
            sub.drawScale = 0.01f;
            sub.targetDrawScale = CARD_SCALE;

            activeSubCards.add(sub);
        }
        updateSubCardDescriptions();
    }

    // 【极简重构】刷新描述文本，不再使用 showEntryText，只看 upgraded 状态
    private void updateSubCardDescriptions() {
        if (activeSubCards.size() < 3 || (activeCard == null && pendingCard == null))
            return;

        // 确定用于提取描述的基准卡牌
        AbstractCard base = (activeCard != null) ? activeCard.asCard() : pendingCard.asCard();
        CardStrings strings = CardCrawlGame.languagePack.getCardStrings(base.cardID);

        if (strings == null || strings.EXTENDED_DESCRIPTION == null)
            return;

        // 检测基准卡是否升级
        boolean isUpgraded = base.upgraded;

        for (int i = 0; i < 3; i++) {
            AbstractCard sub = activeSubCards.get(i);

            // 计算 extended_description 的索引
            // 未升级：子卡0 -> 10, 子卡1 -> 12, 子卡2 -> 14
            // 已升级：子卡0 -> 11, 子卡1 -> 13, 子卡2 -> 15
            int targetIndex = 10 + (i * 2);
            if (isUpgraded) {
                targetIndex += 1; // 往后顺延一位，读取升级后的描述
            }

            if (strings.EXTENDED_DESCRIPTION.length > targetIndex) {
                sub.rawDescription = strings.EXTENDED_DESCRIPTION[targetIndex];
                sub.initializeDescription();
            }
        }
    }

    public void update() {
        if (state == State.IDLE)
            return;

        updateSubCardsLogic();

        switch (state) {
            case EXITING_OLD:
                stateTimer += Gdx.graphics.getDeltaTime();
                for (AbstractCard c : activeSubCards) {
                    c.targetDrawScale = 0.01f;
                    c.target_y = TARGET_Y - 100.0F * Settings.scale;
                }

                if (stateTimer >= FLY_OUT_DURATION) {
                    stateTimer = 0.0f;
                    activeCard = null;
                    activeSubCards.clear();
                    state = State.WAITING;
                }
                break;

            case WAITING:
                stateTimer += Gdx.graphics.getDeltaTime();
                if (stateTimer >= TRANSITION_DELAY) {
                    stateTimer = 0.0f;
                    state = State.ENTERING_NEW;
                    if (pendingCard != null) {
                        initFlyInCoordinates(pendingCard.asCard());
                    }
                }
                break;

            case ENTERING_NEW:
                stateTimer += Gdx.graphics.getDeltaTime();
                float progress = stateTimer / FLY_IN_DURATION;
                if (progress >= 1.0f)
                    progress = 1.0f;

                float[] targetXs = { TARGET_X - SPACING, TARGET_X, TARGET_X + SPACING };

                for (int i = 0; i < 3; i++) {
                    AbstractCard c = activeSubCards.get(i);
                    c.current_x = Interpolation.pow2Out.apply(startX, targetXs[i], progress);
                    c.current_y = Interpolation.pow2Out.apply(startY, TARGET_Y, progress);
                    c.drawScale = Interpolation.linear.apply(0.1f, CARD_SCALE, progress);
                    c.angle = Interpolation.pow2Out.apply(0.0F, 360.0F, progress);
                }

                if (progress >= 1.0f) {
                    stateTimer = 0.0f;
                    activeCard = pendingCard;
                    pendingCard = null;
                    activeCard.onEnterEnvironment();

                    state = State.ACTIVE;
                }
                break;

            case ACTIVE:
                // 【精简】移除了 3 秒定时切换和 updateSubCardDescriptions() 调用，保持静态文本

                float[] lockedXs = { TARGET_X - SPACING, TARGET_X, TARGET_X + SPACING };
                for (int i = 0; i < 3; i++) {
                    AbstractCard c = activeSubCards.get(i);
                    c.target_x = lockedXs[i];
                    c.angle = 0.0F;

                    if (c.hb.hovered) {
                        c.target_y = TARGET_Y + 40.0F * Settings.scale;
                        c.targetDrawScale = HOVER_SCALE;
                    } else {
                        c.target_y = TARGET_Y;
                        c.targetDrawScale = CARD_SCALE;
                    }
                }
                break;
        }
    }

    private void updateSubCardsLogic() {
        for (AbstractCard c : activeSubCards) {
            c.hb.width = AbstractCard.IMG_WIDTH * c.drawScale * Settings.scale;
            c.hb.height = AbstractCard.IMG_HEIGHT * c.drawScale * Settings.scale;
            c.hb.move(c.current_x, c.current_y);
            c.update();
            c.hb.update();
        }
    }

    public void render(SpriteBatch sb) {
        if (activeSubCards.isEmpty())
            return;

        renderBackgroundVFX(sb);

        for (AbstractCard c : activeSubCards) {
            if (!c.hb.hovered) {
                c.render(sb);
            }
        }

        for (AbstractCard c : activeSubCards) {
            if (c.hb.hovered) {
                c.render(sb);
                c.renderCardTip(sb);
            }
        }
    }

    private void renderBackgroundVFX(SpriteBatch sb) {
    }

    public void restoreFromSave(String cardID, boolean upgraded) {
        this.clear();

        AbstractCard base = CardLibrary.getCard(cardID);
        if (base == null)
            return;

        AbstractCard copy = base.makeStatEquivalentCopy();
        if (upgraded) {
            copy.upgrade();
        }

        this.activeCard = (IEnvironmentCard) copy;

        this.startX = TARGET_X;
        createSubCards(copy);

        this.state = State.ACTIVE;

        float[] lockedXs = { TARGET_X - SPACING, TARGET_X, TARGET_X + SPACING };
        for (int i = 0; i < 3; i++) {
            AbstractCard c = activeSubCards.get(i);
            c.current_x = lockedXs[i];
            c.current_y = TARGET_Y;
            c.drawScale = CARD_SCALE;
        }
    }

    public void clear() {
        this.activeCard = null;
        this.pendingCard = null;
        this.activeSubCards.clear();
        this.state = State.IDLE;
        this.stateTimer = 0.0f;
    }
}