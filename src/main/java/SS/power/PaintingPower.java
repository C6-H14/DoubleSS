package SS.power;

import SS.helper.ModHelper;
import SS.interfaces.OnPaintingSubscriber;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.monster.ally.SoulAlly.SoulColor;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.actions.common.ReducePowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class PaintingPower extends AbstractPower {
    private final java.util.Set<UseCardAction> pendingCards = java.util.Collections.newSetFromMap(
            new java.util.IdentityHashMap<UseCardAction, Boolean>());
    private int reserved;
    public static final String POWER_ID = ModHelper.makePath("PaintingPower");
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    private static final String NAME = powerStrings.NAME;
    private static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;

    public PaintingPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.type = AbstractPower.PowerType.BUFF;

        this.amount = amount;

        String path128 = "img/power/PaintingPower84.png";
        String path48 = "img/power/PaintingPower32.png";
        this.region128 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path128), 0, 0, 84, 84);
        this.region48 = new TextureAtlas.AtlasRegion(ImageMaster.loadImage(path48), 0, 0, 32, 32);
        updateDescription();
    }

    @Override
    public void onUseCard(final AbstractCard card, final UseCardAction action) {
        // Reserve existing charges only. Painting gained by this card must not
        // retroactively trigger for the very same play at onAfterUseCard.
        if (amount > reserved && pendingCards.add(action)) reserved++;
    }

    @Override
    public void onAfterUseCard(final AbstractCard card, final UseCardAction action) {
        if (!pendingCards.remove(action)) return;
        addToBot(new com.megacrit.cardcrawl.actions.AbstractGameAction() {
            @Override
            public void update() {
                reserved--;
                applyPainting(card);
                isDone = true;
            }
        });
    }

    private void applyPainting(AbstractCard card) {
        if (amount <= 0 || owner.getPower(ID) != this) return;
        SoulAlly s = null;
        for (AbstractMonster m : AllyManager.allies.monsters) {
            if (m instanceof SoulAlly && !m.isDeadOrEscaped()) {
                s = (SoulAlly) m;
                if (s.stateColor == SoulColor.WHITE) {
                    break;
                }
            }
        }
        if (s == null)
            return;
        this.flash();
        if (card.type == AbstractCard.CardType.ATTACK) {
            s.changeColor(SoulColor.RED);
        } else if (card.type == AbstractCard.CardType.SKILL) {
            s.changeColor(SoulColor.GREEN);
        } else if (card.type == AbstractCard.CardType.POWER) {
            s.changeColor(SoulColor.BLUE);
        } else {
            s.changeColor(SoulColor.PURPLE);
        }
        this.addToTop(new ReducePowerAction(this.owner, this.owner, this, 1));
        for (AbstractCard c : new java.util.ArrayList<>(AbstractDungeon.player.discardPile.group)) {
            if (c instanceof OnPaintingSubscriber) {
                ((OnPaintingSubscriber) c).triggerOnPainting(c);
            }
        }
    }

    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
}
