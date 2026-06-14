package SS.power;

import SS.Dice.AttackDice;
import SS.Dice.DefendDice;
import SS.Dice.IronwaveDice;
import SS.action.monster.EvokeSoulAction;
import SS.helper.ModHelper;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.monster.ally.SoulAlly.SoulColor;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;

public class SunbowPower extends AbstractPower {
    public static final String POWER_ID = ModHelper.makePath("SunbowPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public SunbowPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/SunbowPower84.png";
        String path48 = "img/power/SunbowPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    public void atStartOfTurn() {
        addToBot(new EvokeSoulAction(this.amount));
        for (AbstractMonster mo : AllyManager.allies.monsters) {
            if (mo instanceof SoulAlly) {
                SoulAlly soulAlly = (SoulAlly) mo;
                SoulColor color = soulAlly.stateColor;
                if (AbstractDungeon.player.drawPile.group.isEmpty()) {
                    continue;
                }
                AbstractCard c = AbstractDungeon.player.drawPile.getTopCard();
                switch (c.type) {
                    case ATTACK:
                        if (color != SoulColor.RED) {
                            soulAlly.changeColor(SoulColor.RED);
                        }
                        break;
                    case SKILL:
                        if (color != SoulColor.GREEN) {
                            soulAlly.changeColor(SoulColor.GREEN);
                        }
                        break;
                    case POWER:
                        if (color != SoulColor.BLUE) {
                            soulAlly.changeColor(SoulColor.BLUE);
                        }
                        break;
                    case STATUS:
                        if (color != SoulColor.PURPLE) {
                            soulAlly.changeColor(SoulColor.PURPLE);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }

}
