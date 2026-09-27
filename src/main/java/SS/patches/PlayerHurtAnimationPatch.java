package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

import SS.animation.CharacterAnimationController;
import SS.animation.CharacterAnimationRequest;
import SS.characters.AbstractSSCharacter;

/** Plays the first-stage hurt animation only when player HP actually decreases. */
@SpirePatch(clz = AbstractPlayer.class, method = "damage")
public class PlayerHurtAnimationPatch {
    private static int healthBefore;

    public static void Prefix(AbstractPlayer __instance, DamageInfo info) {
        healthBefore = __instance.currentHealth;
    }

    public static void Postfix(AbstractPlayer __instance, DamageInfo info) {
        // Lethal damage invokes playDeathAnimation() inside AbstractPlayer.damage().
        // Do not overwrite that animation with hurt when this Postfix runs.
        if (__instance instanceof AbstractSSCharacter
                && __instance.currentHealth < healthBefore
                && __instance.currentHealth > 0
                && !__instance.isDying
                && !__instance.isDead) {
            CharacterAnimationController.play(CharacterAnimationController.HURT,
                    CharacterAnimationRequest.Policy.FORCE_RESTART);
        }
    }
}
