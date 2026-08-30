package SS.Dice;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

import SS.helper.ModHelper;

/**
 * 骰子空充能槽。
 *
 * 继承原版的 {@link EmptyOrbSlot}（而不是 AbstractDice）：原版约 15 处"instanceof
 * EmptyOrbSlot"判空槽的检查（AbstractPlayer.evokeOrb / hasEmptyOrb / filledOrbCount /
 * channelOrb、Cables 遗物、ChannelAction、Flux/Blaster/Impulse/Redo 等）就能自然把
 * 它认作空槽 —— 骰子给 Defect 等其他角色时，不会再把空骰槽误当成真球。
 *
 * 保留 mod 自己的 ID（"Double:Empty"，名称/描述走 orb.json）；贴图、onEvoke 空实现
 * 全部直接继承原版（与旧版渲染一致）。
 */
public class EmptyDiceSlot extends EmptyOrbSlot {
    public static final String ORB_ID = ModHelper.makePath("Empty");
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString(ORB_ID);
    public static final String[] DESC = orbString.DESCRIPTION;

    public EmptyDiceSlot(float x, float y) {
        super(x, y);
        this.ID = ORB_ID;
        this.name = orbString.NAME;
        this.updateDescription();
    }

    public EmptyDiceSlot() {
        super();
        this.ID = ORB_ID;
        this.name = orbString.NAME;
        this.updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = DESC[0];
    }

    @Override
    public void updateAnimation() {
        super.updateAnimation();
        // 旧版空槽经由 AbstractDice.updateAnimation 额外 angle += dt*2；现继承
        // EmptyOrbSlot（仅 dt*10），补回 dt*2 让空槽旋转速度与旧版一致。
        this.angle += Gdx.graphics.getDeltaTime() * 2.0F;
    }

    @Override
    public AbstractOrb makeCopy() {
        return new EmptyDiceSlot();
    }
}
