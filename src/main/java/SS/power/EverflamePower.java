package SS.power;

import SS.cards.Lost.AbstractLostCard;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.path.AbstractCardEnum;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class EverflamePower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("EverflamePower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public EverflamePower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/EverflamePower84.png";
        String path48 = "img/power/EverflamePower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            boolean hasSoulFire = false;
            // 遍历你自己的 AllyManager (或者 AbstractDungeon.getMonsters())
            for (AbstractMonster m : AllyManager.allies.monsters) {
                if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                    hasSoulFire = true;
                }
            }
            if (hasSoulFire) {
                ModHelper.atbLambda(() -> {
                    for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
                        if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                            ((AbstractLostCard) c).upgradePermanentDamage(amount);
                            ((AbstractLostCard) c).upgradePermanentBlock(amount);
                            ((AbstractLostCard) c).upgradePermanentMagicNumber(amount);
                        }
                    }
                    for (AbstractCard c : AbstractDungeon.player.hand.group) {
                        if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                            ((AbstractLostCard) c).upgradePermanentDamage(amount);
                            ((AbstractLostCard) c).upgradePermanentBlock(amount);
                            ((AbstractLostCard) c).upgradePermanentMagicNumber(amount);
                        }
                    }
                    for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
                        if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                            ((AbstractLostCard) c).upgradePermanentDamage(amount);
                            ((AbstractLostCard) c).upgradePermanentBlock(amount);
                            ((AbstractLostCard) c).upgradePermanentMagicNumber(amount);
                        }
                    }
                    for (AbstractCard c : AbstractDungeon.player.exhaustPile.group) {
                        if (c instanceof AbstractLostCard && c.hasTag(AbstractCardEnum.Permanent)) {
                            ((AbstractLostCard) c).upgradePermanentDamage(amount);
                            ((AbstractLostCard) c).upgradePermanentBlock(amount);
                            ((AbstractLostCard) c).upgradePermanentMagicNumber(amount);
                        }
                    }
                });
            }
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + amount + DESCRIPTIONS[1];
    }

}
