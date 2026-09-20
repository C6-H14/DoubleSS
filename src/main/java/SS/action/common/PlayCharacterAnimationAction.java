package SS.action.common;

import java.util.Arrays;
import java.util.List;

import com.megacrit.cardcrawl.actions.AbstractGameAction;

import SS.animation.CharacterAnimationController;
import SS.animation.CharacterAnimationRequest;

/** Queue-friendly hook for cards that need one or more explicit character actions. */
public class PlayCharacterAnimationAction extends AbstractGameAction {
    private final CharacterAnimationRequest request;

    public PlayCharacterAnimationAction(String animation) {
        this(animation, CharacterAnimationRequest.Policy.INTERRUPT);
    }

    public PlayCharacterAnimationAction(String animation, CharacterAnimationRequest.Policy policy) {
        this.request = new CharacterAnimationRequest(animation, policy);
    }

    public PlayCharacterAnimationAction(CharacterAnimationRequest.Policy policy, String... animations) {
        this(Arrays.asList(animations), policy);
    }

    public PlayCharacterAnimationAction(List<String> animations, CharacterAnimationRequest.Policy policy) {
        this.request = new CharacterAnimationRequest(animations, policy);
    }

    @Override
    public void update() {
        CharacterAnimationController.play(request);
        this.isDone = true;
    }
}
