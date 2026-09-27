package SS.characters;

/**
 * Character artwork/runtime variants.
 *
 * Keeping the resource roots here prevents experimental inner-character
 * animations from overwriting the production front-character runtime.
 */
public enum CharacterVisualForm {
    FRONT("img/char/spine/character.atlas", "img/char/spine/character.json"),
    INNER("img/char/spine_inner/character.atlas", "img/char/spine_inner/character.json");

    public final String atlasPath;
    public final String skeletonPath;

    CharacterVisualForm(String atlasPath, String skeletonPath) {
        this.atlasPath = atlasPath;
        this.skeletonPath = skeletonPath;
    }
}
