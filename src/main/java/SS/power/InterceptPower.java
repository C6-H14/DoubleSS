package SS.power;

import SS.helper.ModHelper;
import SS.interfaces.IAllyDamageModifier;
import SS.monster.ally.AbstractAlly;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.VulnerablePower;
import com.megacrit.cardcrawl.powers.WeakPower;

public class InterceptPower extends AbstractPower implements IAllyDamageModifier {
    public static final String POWER_ID = ModHelper.makePath("InterceptPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public InterceptPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/InterceptPower84.png";
        String path48 = "img/power/InterceptPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    @Override
    public float onAllyModifyDamageFinal(AbstractAlly ally, AbstractCreature target, float damage,
            DamageInfo.DamageType type) {
        // 判断条件：普通物理伤害 + 目标同时有易伤和虚弱
        if (type == DamageInfo.DamageType.NORMAL && target != null) {
            boolean hasVuln = target.hasPower(VulnerablePower.POWER_ID);
            boolean hasWeak = target.hasPower(WeakPower.POWER_ID); // 注意是 "Weakened"

            if (hasVuln && hasWeak) {
                return damage * 2.0F; // 直接翻倍！
            }
        }
        return damage;
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + amount + DESCRIPTIONS[1];
    }
}