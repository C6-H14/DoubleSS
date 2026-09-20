package SS.animation;

import java.util.Arrays;
import java.util.List;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import SS.characters.AbstractSSCharacter;

/** Single entry point for character animation requests. */
public final class CharacterAnimationController {
    public static final String IDLE = "idle";
    public static final String ATTACK_SWORD = "attack_sword";
    public static final String CAST = "cast";
    public static final String CAST_ATTACK_1 = "cast_attack_1";
    public static final String CAST_ATTACK_2 = "cast_attack_2";
    public static final String ATTACK_HEAVY = "attack_heavy";
    public static final String CAST_DEBUFF = "cast_debuff";
    public static final String CAST_BUFF = "cast_buff";
    public static final String CAST_PRAYER = "cast_prayer";

    private CharacterAnimationController() {
    }

    public static boolean play(CharacterAnimationRequest request) {
        if (request == null || AbstractDungeon.player == null
                || !(AbstractDungeon.player instanceof AbstractSSCharacter)) {
            return false;
        }
        return ((AbstractSSCharacter) AbstractDungeon.player).playCharacterAnimation(request);
    }

    public static boolean play(String animation, CharacterAnimationRequest.Policy policy) {
        return play(new CharacterAnimationRequest(animation, policy));
    }

    public static boolean playSequence(CharacterAnimationRequest.Policy policy, String... animations) {
        return playSequence(Arrays.asList(animations), policy, IDLE);
    }

    public static boolean playSequence(List<String> animations, CharacterAnimationRequest.Policy policy) {
        return playSequence(animations, policy, IDLE);
    }

    public static boolean playSequence(List<String> animations, CharacterAnimationRequest.Policy policy,
            String fallbackAnimation) {
        return play(new CharacterAnimationRequest(animations, policy, fallbackAnimation));
    }

    public static boolean resetToIdle() {
        if (AbstractDungeon.player == null || !(AbstractDungeon.player instanceof AbstractSSCharacter)) {
            return false;
        }
        return ((AbstractSSCharacter) AbstractDungeon.player).resetCharacterAnimation();
    }
}
