package SS.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable request used by cards and actions to drive the combat character. */
public final class CharacterAnimationRequest {
    public enum Policy {
        IGNORE_IF_BUSY,
        INTERRUPT,
        FORCE_RESTART
    }

    public final List<String> animations;
    public final Policy policy;
    public final String fallbackAnimation;

    public CharacterAnimationRequest(String animation, Policy policy) {
        this(Collections.singletonList(animation), policy, CharacterAnimationController.IDLE);
    }

    public CharacterAnimationRequest(List<String> animations, Policy policy) {
        this(animations, policy, CharacterAnimationController.IDLE);
    }

    public CharacterAnimationRequest(List<String> animations, Policy policy, String fallbackAnimation) {
        ArrayList<String> copy = new ArrayList<>();
        if (animations != null) {
            for (String animation : animations) {
                if (animation != null && !animation.trim().isEmpty()) {
                    copy.add(animation.trim());
                }
            }
        }
        this.animations = Collections.unmodifiableList(copy);
        this.policy = policy;
        this.fallbackAnimation = fallbackAnimation;
    }
}
