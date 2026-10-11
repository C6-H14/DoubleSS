package SS.characters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import basemod.abstracts.CustomPlayer;
import basemod.ReflectionHacks;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.AnimationStateData;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.defect.AnimateOrbAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.defect.EvokeOrbAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;

import SS.Dice.EmptyDiceSlot;
import SS.animation.CharacterAnimationController;
import SS.animation.CharacterAnimationDiagnostics;
import SS.animation.CharacterAnimationRequest;
import SS.cards.AbstractDoubleCard;
import SS.cards.MultiFacial;
import SS.modcore.modcore;
import SS.packages.AbstractPackage;
import SS.path.PackageEnumList.PackageEnum;

/**
 * 二硫键系角色基类。
 *
 * 把所有"机制"（骰子/充能球充能、卡包卡池、选角色特效、罪值条相关、
 * 起始事件卡、斯派尔之心攻击动作等）收敛到这一层，具体角色（MyCharacter
 * 及未来的变种）只提供身份差异：颜色枚举、起始牌组/圣物、本地化名称、
 * cutscene、@SpireEnum 注册等。
 *
 * 凡是判断"是否二硫键系"的地方（回合结束自动激发、颢牌掉落、卡包选择流程、
 * 罪值条渲染、人物选择自定义卡包 UI）都应 {@code instanceof AbstractSSCharacter}
 * 而不是某个具体角色，这样新变种自动继承整套机制，无需再改这些门槛。
 */
public abstract class AbstractSSCharacter extends CustomPlayer {
    private boolean characterAnimationLoaded;
    private boolean characterDeathAnimationStarted;
    private boolean usesSharedAnimationAtlas;
    // Resource ownership belongs to this cache, not to any player instance.
    // Loading and disposal run on the game's GL thread.
    private static final Map<String, CachedAnimation> ANIMATION_CACHE = new HashMap<>();
    private static boolean cacheLifecycleRegistered;

    private static final class CachedAnimation {
        final TextureAtlas atlas;
        final SkeletonData skeletonData;

        CachedAnimation(TextureAtlas atlas, SkeletonData skeletonData) {
            this.atlas = atlas;
            this.skeletonData = skeletonData;
        }
    }

    private static void registerCacheLifecycle() {
        if (cacheLifecycleRegistered) {
            return;
        }
        Gdx.app.addLifecycleListener(new LifecycleListener() {
            @Override public void pause() { }
            @Override public void resume() { }
            @Override public void dispose() {
                for (CachedAnimation resource : ANIMATION_CACHE.values()) {
                    resource.atlas.dispose();
                }
                ANIMATION_CACHE.clear();
                cacheLifecycleRegistered = false;
            }
        });
        cacheLifecycleRegistered = true;
    }

    /**
     * 转发给 CustomPlayer 的构造器（orbTextures/orbVfx/layerSpeed 那套带充能球动画的签名），
     * 子类 super(...) 一行不用改即可挂到基类。
     */
    protected AbstractSSCharacter(String name, AbstractPlayer.PlayerClass playerClass,
            String[] orbTextures, String orbVfxUrl, float[] layerSpeed, String cutscene, String relicList) {
        super(name, playerClass, orbTextures, orbVfxUrl, layerSpeed, cutscene, relicList);
    }

    protected final void initializeCharacterAnimation(String atlas, String skeletonJson, float scale) {
        characterDeathAnimationStarted = false;
        final long started = System.nanoTime();
        final String cacheKey = getClass().getName() + "|" + atlas + "|" + skeletonJson
                + "|" + Float.floatToIntBits(scale) + "|" + Float.floatToIntBits(Settings.renderScale);
        boolean cacheHit = false;
        CharacterAnimationDiagnostics.log("load atlas=" + atlas + " skeleton=" + skeletonJson
                + " scale=" + scale + " codeSource="
                + CharacterAnimationDiagnostics.codeSource(getClass())
                + " logPath=" + CharacterAnimationDiagnostics.logPath());
        try {
            registerCacheLifecycle();
            CachedAnimation cached = ANIMATION_CACHE.get(cacheKey);
            cacheHit = cached != null;
            if (cacheHit) {
                this.atlas = cached.atlas;
                usesSharedAnimationAtlas = true;
                // Never share mutable pose, listeners, queues, or mix settings.
                this.skeleton = new Skeleton(cached.skeletonData);
                this.skeleton.setColor(Color.WHITE);
                this.stateData = new AnimationStateData(cached.skeletonData);
                this.state = new AnimationState(this.stateData);
            } else {
                usesSharedAnimationAtlas = false;
                // Retain the original loader (including other mods' patches)
                // for the first load. Publish only after idle validation.
                loadAnimation(atlas, skeletonJson, scale);
            }
            characterAnimationLoaded = this.state != null && this.state.getData() != null;
            if (characterAnimationLoaded) {
                this.state.addListener(new AnimationState.AnimationStateListener() {
                    @Override
                    public void start(int trackIndex) {
                        AnimationState.TrackEntry entry = state.getCurrent(trackIndex);
                        String name = entry == null || entry.getAnimation() == null
                                ? "none" : entry.getAnimation().getName();
                        if (!CharacterAnimationController.IDLE.equals(name)) {
                            CharacterAnimationDiagnostics.log("spine-start track=" + trackIndex
                                    + " animation=" + name + " stack="
                                    + CharacterAnimationDiagnostics.stack(16));
                        }
                    }

                    @Override
                    public void event(int trackIndex, Event event) {
                        CharacterAnimationDiagnostics.log("spine-event track=" + trackIndex
                                + " event=" + event.getData().getName());
                    }

                    @Override
                    public void complete(int trackIndex, int loopCount) {
                        // Start events are sufficient; avoid per-loop idle spam.
                    }

                    @Override
                    public void end(int trackIndex) {
                    }
                });
                Animation idle = this.state.getData().getSkeletonData()
                        .findAnimation(CharacterAnimationController.IDLE);
                if (idle == null) {
                    throw new IllegalStateException("Character runtime is missing idle animation");
                }
                this.state.setAnimation(0, idle, true);
                if (!cacheHit) {
                    ANIMATION_CACHE.put(cacheKey, new CachedAnimation(this.atlas,
                            this.state.getData().getSkeletonData()));
                    usesSharedAnimationAtlas = true;
                }
                CharacterAnimationDiagnostics.log("load-result=ok cast_s="
                        + (this.state.getData().getSkeletonData().findAnimation("cast_s") != null)
                        + " cast_special="
                        + (this.state.getData().getSkeletonData().findAnimation("cast_special") != null)
                        + " cache=" + (cacheHit ? "hit" : "miss")
                        + " elapsedMs=" + (System.nanoTime() - started) / 1000000L);
            }
        } catch (RuntimeException error) {
            characterAnimationLoaded = false;
            // loadAnimation assigns atlas before it parses the skeleton. If a
            // later texture/JSON load fails, leaving that partial atlas in
            // place makes AbstractPlayer call state.update() with state ==
            // null on the first rendered combat frame.
            if (this.atlas != null && !usesSharedAnimationAtlas) {
                try {
                    this.atlas.dispose();
                } catch (RuntimeException ignored) {
                    // Best-effort cleanup; the important part is nulling it.
                }
            }
            this.atlas = null;
            this.state = null;
            this.stateData = null;
            this.skeleton = null;
            usesSharedAnimationAtlas = false;
            System.err.println("[DoubleSS] Failed to load character animation; using static fallback:");
            CharacterAnimationDiagnostics.log("load-result=failed error=" + error);
            error.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        // AbstractPlayer.dispose normally destroys its atlas. Detach the
        // cache-owned atlas before delegating; keep normal static-image cleanup.
        if (usesSharedAnimationAtlas) {
            this.atlas = null;
            usesSharedAnimationAtlas = false;
        }
        characterAnimationLoaded = false;
        super.dispose();
        this.state = null;
        this.stateData = null;
        this.skeleton = null;
    }

    /** Loads one explicitly selected visual form. */
    protected final void initializeCharacterAnimation(CharacterVisualForm form, float scale) {
        if (form == null) {
            throw new IllegalArgumentException("Character visual form must not be null");
        }
        initializeCharacterAnimation(form.atlasPath, form.skeletonPath, scale);
    }

    public final boolean playCharacterAnimation(CharacterAnimationRequest request) {
        if (characterDeathAnimationStarted) {
            return false;
        }
        if (!characterAnimationLoaded || request == null || request.animations.isEmpty()) {
            CharacterAnimationDiagnostics.log("result=rejected loaded=" + characterAnimationLoaded
                    + " request=" + (request == null ? "null" : request.animations));
            return false;
        }

        ArrayList<String> available = new ArrayList<>();
        for (String animationName : request.animations) {
            Animation animation = this.state.getData().getSkeletonData().findAnimation(animationName);
            if (animation != null) {
                available.add(animationName);
            } else {
                System.err.println("[DoubleSS] Missing character animation; skipping: " + animationName);
                CharacterAnimationDiagnostics.log("missing-animation=" + animationName);
            }
        }
        if (available.isEmpty()) {
            String fallback = request.fallbackAnimation;
            if (fallback != null && this.state.getData().getSkeletonData().findAnimation(fallback) != null) {
                available.add(fallback);
                CharacterAnimationDiagnostics.log("fallback-used=" + fallback);
            } else {
                CharacterAnimationDiagnostics.log("result=reset-idle no-available-animation");
                return resetCharacterAnimation();
            }
        }

        AnimationState.TrackEntry current = this.state.getCurrent(0);
        boolean busy = current != null && current.getAnimation() != null
                && !CharacterAnimationController.IDLE.equals(current.getAnimation().getName())
                && !current.isComplete();
        if (request.policy == CharacterAnimationRequest.Policy.IGNORE_IF_BUSY && busy) {
            CharacterAnimationDiagnostics.log("result=ignored-busy current="
                    + current.getAnimation().getName() + " requested=" + request.animations);
            return false;
        }
        if (request.policy != CharacterAnimationRequest.Policy.FORCE_RESTART && available.size() == 1
                && current != null && current.getAnimation() != null
                && available.get(0).equals(current.getAnimation().getName()) && !current.isComplete()) {
            CharacterAnimationDiagnostics.log("result=ignored-already-playing current="
                    + current.getAnimation().getName());
            return false;
        }

        CharacterAnimationDiagnostics.log("result=playing selected=" + available
                + " previous=" + (current == null || current.getAnimation() == null
                        ? "none" : current.getAnimation().getName())
                + " caller=" + CharacterAnimationDiagnostics.caller());
        // Pass the exact Animation object. The loadout mod intercepts the
        // String overload with substring matching, so "cast_s" otherwise
        // resolves to the earlier "cast_special" animation.
        Animation first = this.state.getData().getSkeletonData().findAnimation(available.get(0));
        this.state.setAnimation(0, first, false);
        for (int i = 1; i < available.size(); ++i) {
            Animation next = this.state.getData().getSkeletonData().findAnimation(available.get(i));
            this.state.addAnimation(0, next, false, 0.0F);
        }
        Animation idle = this.state.getData().getSkeletonData()
                .findAnimation(CharacterAnimationController.IDLE);
        this.state.addAnimation(0, idle, true, 0.0F);
        return true;
    }

    /** Death is one-shot and holds its final pose instead of queuing idle. */
    @Override
    public void playDeathAnimation() {
        if (characterDeathAnimationStarted) {
            return;
        }
        Animation death = characterAnimationLoaded && this.state != null
                ? this.state.getData().getSkeletonData().findAnimation("death") : null;
        characterDeathAnimationStarted = true;
        if (death == null) {
            CharacterAnimationDiagnostics.log("death-result=static-fallback missing-animation=death");
            super.playDeathAnimation();
            return;
        }
        // renderCorpse=true bypasses Spine and draws the static corpse texture.
        ReflectionHacks.setPrivate(this, AbstractPlayer.class, "renderCorpse", false);
        this.state.clearTracks();
        this.state.setAnimation(0, death, false);
        CharacterAnimationDiagnostics.log("death-result=playing animation=death loop=false");
    }

    /** Clears stale queued TrackEntry objects after loading/changing rooms. */
    public final boolean resetCharacterAnimation() {
        if (characterDeathAnimationStarted) {
            return false;
        }
        if (!characterAnimationLoaded || this.state == null) {
            return false;
        }
        Animation idle = this.state.getData().getSkeletonData().findAnimation(CharacterAnimationController.IDLE);
        if (idle == null) {
            return false;
        }
        this.state.clearTracks();
        this.state.setAnimation(0, idle, true);
        return true;
    }

    public final boolean hasCharacterAnimation() {
        return characterAnimationLoaded;
    }

    public int getAscensionMaxHPLoss() {
        return 7;
    }

    public AbstractCard getStartCardForEvent() {
        return new MultiFacial();
    }

    public BitmapFont getEnergyNumFont() {
        return FontHelper.energyNumFontBlue;
    }

    public void doCharSelectScreenSelectEffect() {
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }

    @Override
    public void channelOrb(AbstractOrb orbToSet)// 产生骰子
    {
        if (orbToSet instanceof EmptyOrbSlot)
            return; // EmptyDiceSlot 继承 EmptyOrbSlot，一个判断同时覆盖两种空槽
        // 骰子和原版充能球都允许充能：回合结束时由 EvokeAllPatch 统一自动激发
        if (this.maxOrbs <= 0) {
            AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0F, MSG[4], true));
        } else {
            int index = -1;
            for (int i = 0; i < this.orbs.size(); i++) {
                if (this.orbs.get(i) instanceof EmptyOrbSlot) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                orbToSet.cX = ((AbstractOrb) this.orbs.get(index)).cX;
                orbToSet.cY = ((AbstractOrb) this.orbs.get(index)).cY;
                this.orbs.set(index, orbToSet);
                ((AbstractOrb) this.orbs.get(index)).setSlot(index, this.maxOrbs);
                orbToSet.playChannelSFX();
                for (AbstractPower p : this.powers)
                    p.onChannel(orbToSet);
                AbstractDungeon.actionManager.orbsChanneledThisCombat.add(orbToSet);
                AbstractDungeon.actionManager.orbsChanneledThisTurn.add(orbToSet);
            } else {
                AbstractDungeon.actionManager.addToTop(new ChannelAction(orbToSet));
                AbstractDungeon.actionManager.addToTop(new EvokeOrbAction(1));
                AbstractDungeon.actionManager.addToTop(new AnimateOrbAction(1));
            }
        }
    }

    public void increaseMaxOrbSlots(int amount, boolean playSfx) {
        if (this.maxOrbs >= 5) {
            AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0F, "槽位已满！", true));
        } else {
            if (playSfx)
                CardCrawlGame.sound.play("ORB_SLOT_GAIN", 0.1F);
            this.maxOrbs += amount;
            int i;
            for (i = 0; i < amount; i++) {
                this.orbs.add(new EmptyDiceSlot());
            }
            for (i = 0; i < this.orbs.size(); i++) {
                ((AbstractOrb) this.orbs.get(i)).setSlot(i, this.maxOrbs);
            }
        }
    }

    public void evokeOrb() {
        if (!this.orbs.isEmpty() && !(this.orbs.get(0) instanceof EmptyOrbSlot)) {
            ((AbstractOrb) this.orbs.get(0)).onEvoke();
            AbstractOrb orbSlot = new EmptyDiceSlot();

            int i;
            for (i = 1; i < this.orbs.size(); i++) {
                Collections.swap(this.orbs, i, i - 1);
            }

            this.orbs.set(this.orbs.size() - 1, orbSlot);

            for (i = 0; i < this.orbs.size(); i++) {
                ((AbstractOrb) this.orbs.get(i)).setSlot(i, this.maxOrbs);
            }
        }
    }

    public AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect() {
        return new AbstractGameAction.AttackEffect[] { AbstractGameAction.AttackEffect.SLASH_HEAVY,
                AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.SLASH_DIAGONAL,
                AbstractGameAction.AttackEffect.SLASH_HEAVY,
                AbstractGameAction.AttackEffect.FIRE, AbstractGameAction.AttackEffect.SLASH_DIAGONAL };
    }

    public ArrayList<AbstractCard> getCardPool(ArrayList<AbstractCard> tmpPool) {
        ArrayList<AbstractCard> poolCards = new ArrayList<>();
        ArrayList<AbstractCard> allowedCards = new ArrayList<>();
        for (AbstractPackage p : modcore.validPackage) {
            System.out.println(p.ID);
            if (p.ID.equals("Double:NullPackage")) {
                continue;
            }
            allowedCards.addAll(p.CardLists);
        }
        // 卡包协同卡（有序对枚举解锁，见 modcore.buildPairCards）
        allowedCards.addAll(modcore.pairCardPool);
        for (AbstractCard card : CardLibrary.getAllCards()) {
            // 自己的颜色（基类用 this.getCardColor()，SS 即 SS_Yellow，变种用各自颜色）：
            // 只放入 Default 卡包的牌，避免同色卡包牌重复
            if (card.color == this.getCardColor()) {
                if (card instanceof AbstractDoubleCard) {
                    AbstractDoubleCard d = (AbstractDoubleCard) card;
                    if (d.packagetype == PackageEnum.Default) {
                        allowedCards.add(card.makeStatEquivalentCopy());
                    }
                }
                continue;
            }
            if (card.color == AbstractDungeon.player.getCardColor()) {
                allowedCards.add(card.makeStatEquivalentCopy());
            }
        }
        System.out.println("allowedCards:");
        for (AbstractCard c : allowedCards) {
            System.out.println(c);
            poolCards.add(c);
            switch (c.rarity) {
                case COMMON:
                    AbstractDungeon.commonCardPool.addToTop(c);
                    break;
                case UNCOMMON:
                    AbstractDungeon.uncommonCardPool.addToTop(c);
                    break;
                case RARE:
                    AbstractDungeon.rareCardPool.addToTop(c);
                    break;
            }
        }
        return poolCards;
    }
}
