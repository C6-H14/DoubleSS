package SS.action.unique.c6h14;

import SS.action.common.EchoACardAction;
import SS.action.common.PlayCardAction;
import SS.cardmodifiers.EchoTagModifier;
import SS.monster.ally.AllyManager;
import SS.monster.ally.SoulAlly;
import SS.path.AbstractCardEnum;
import SS.power.InscribeCardPower;
import basemod.helpers.CardModifierManager;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class EchoInscriptionsAction extends AbstractGameAction {
    @Override
    public void update() {
        for (AbstractMonster ally : AllyManager.allies.monsters) {
            if (!(ally instanceof SoulAlly) || ally.isDeadOrEscaped()) continue;
            if (!(ally.getPower(InscribeCardPower.POWER_ID) instanceof InscribeCardPower)) continue;
            AbstractCard original = ((InscribeCardPower) ally.getPower(InscribeCardPower.POWER_ID)).card;
            if (original == null || original.hasTag(AbstractCardEnum.Echo)
                    || CardModifierManager.hasModifier(original, EchoTagModifier.ID)) continue;
            // Use the same playback path as InscribeCardPower, without releasing
            // its stored card. Only the temporary echo is exhausted after play.
            AbstractCard echo = EchoACardAction.buildEcho(original, true);
            addToBot(new PlayCardAction(AbstractDungeon.getCurrRoom().monsters
                    .getRandomMonster(null, true, AbstractDungeon.cardRandomRng), echo, true));
        }
        isDone = true;
    }
}
