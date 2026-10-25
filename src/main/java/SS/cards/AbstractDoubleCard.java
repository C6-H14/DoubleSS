package SS.cards;

import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.evacipated.cardcrawl.mod.stslib.fields.cards.AbstractCard.ExhaustiveField;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.DamageInfo.DamageType;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import SS.helper.CardUtil;
import SS.helper.ConciseText;
import SS.path.AbstractCardEnum;
import SS.path.PackageEnumList.PackageEnum;
import basemod.abstracts.CustomCard;

public abstract class AbstractDoubleCard extends CustomCard {
    public AbstractDoubleCard fatherCard = this;
    protected CardStrings CARD_STRINGS;
    protected String[] EXTENDED_DESCRIPTION = { "", "", "", "", "", "" };
    protected boolean isFiend = false, isManager = false;
    private final Boolean codexManagerPreview = SS.helper.CardPreviewContext.managerMode();
    public PackageEnum packagetype;
    /**
     * 开局选卡包时，本选项卡（OptionCard 的副本）对应哪个子卡包。
     * 只在 addClassChoice 里对副本赋值，选包结束即失效，不参与存盘。
     * 存在的理由：第三轮保护机制允许同一颜色占两个槽位，此时
     * 「颜色 → 子包」的映射会被后者覆盖，必须靠选项卡实例本身来区分。
     */
    public SS.packages.AbstractPackage choiceSubPackage;

    /** Override with false when a card manages all of its character actions manually. */
    public boolean shouldPlayDefaultCharacterAnimation() {
        return true;
    }

    /** Override to replace the normal sword swing with another named animation. */
    public String getDefaultCharacterAnimation() {
        return null;
    }

    /**
     * Override when one card use should queue several character animations.
     * Repeated names are allowed, for example cast -> attack_sword -> attack_sword.
     */
    public List<String> getDefaultCharacterAnimationSequence() {
        String animation = getDefaultCharacterAnimation();
        return animation == null || animation.isEmpty()
                ? Collections.<String>emptyList()
                : Collections.singletonList(animation);
    }

    public AbstractDoubleCard findFather() {
        if (this.fatherCard == null)
            this.fatherCard = this;
        if (this.fatherCard == this)
            return this;
        this.fatherCard = this.fatherCard.findFather();
        return this.fatherCard;
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, PackageEnum pe, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, AbstractCardEnum.SS_Yellow, rarity, target);
        this.packagetype = pe;
        repaint();
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, PackageEnum pe, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, AbstractCardEnum.SS_Yellow, rarity, target);
        this.packagetype = pe;
        repaint();
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, PackageEnum pe, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, AbstractCardEnum.SS_Yellow, rarity, target);
        this.packagetype = pe;
        repaint();
        CARD_STRINGS = card_string;
        EXTENDED_DESCRIPTION = exstrings;
        isFiend = fiend;
        isManager = manager;
        fatherCard = this;
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, PackageEnum pe, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, AbstractCardEnum.SS_Yellow, rarity, target);
        this.packagetype = pe;
        repaint();
        CARD_STRINGS = card_string;
        EXTENDED_DESCRIPTION = exstrings;
        isFiend = fiend;
        isManager = manager;
        fatherCard = this;
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.packagetype = PackageEnum.Default;
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.packagetype = PackageEnum.Default;
        fatherCard = this;
    }

    public AbstractDoubleCard(String id, String name, String img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.packagetype = PackageEnum.Default;
        CARD_STRINGS = card_string;
        EXTENDED_DESCRIPTION = exstrings;
        isFiend = fiend;
        isManager = manager;
        fatherCard = this;
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    public AbstractDoubleCard(String id, String name, RegionName img, int cost, String rawDescription,
            AbstractCard.CardType type, AbstractCard.CardColor color, AbstractCard.CardRarity rarity,
            AbstractCard.CardTarget target, CardStrings card_string, String[] exstrings, boolean fiend,
            boolean manager) {
        super(id, name, img, cost, rawDescription, type, color, rarity, target);
        this.packagetype = PackageEnum.Default;
        CARD_STRINGS = card_string;
        EXTENDED_DESCRIPTION = exstrings;
        isFiend = fiend;
        isManager = manager;
        fatherCard = this;
        if (fiend)
            this.tags.add(AbstractCardEnum.Fiend);
        if (manager)
            this.tags.add(AbstractCardEnum.Manager);
    }

    private void repaint() {
        if (this.packagetype == PackageEnum.Default || this.packagetype == PackageEnum.SS) {
            return;
        }
        if (this.packagetype == PackageEnum.RED || this.packagetype == PackageEnum.GREEN
                || this.packagetype == PackageEnum.BLUE || this.packagetype == PackageEnum.PURPLE) {
            return;
        }
        String s = this.packagetype.toString();
        this.setOrbTexture("img/512/" + s + "_orb.png", "img/1024/" + s + "_orb.png");
        switch (this.type) {
            case ATTACK:
                this.setBackgroundTexture("img/512/" + s + "_attack.png", "img/1024/" + s + "_attack.png");
                break;
            case SKILL:
                this.setBackgroundTexture("img/512/" + s + "_skill.png", "img/1024/" + s + "_skill.png");
                break;
            case POWER:
                this.setBackgroundTexture("img/512/" + s + "_power.png", "img/1024/" + s + "_power.png");
                break;
            default:
                this.setBackgroundTexture("img/512/" + s + "_skill.png", "img/1024/" + s + "_skill.png");
                break;
        }
    }

    protected void setDamage(int amount) {
        this.damage = this.baseDamage = amount;
    }

    protected void setBlock(int amount) {
        this.block = this.baseBlock = amount;
    }

    protected void setMagic(int amount) {
        this.magicNumber = this.baseMagicNumber = amount;
    }

    protected void selfPower(AbstractPower p) {
        addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, p));
    }

    protected void enemyPower(AbstractPower p, AbstractMonster m) {
        addToBot(new ApplyPowerAction(m, m, p));
    }

    protected void allEnemyPower(AbstractPower p) {
        for (AbstractMonster mo : (AbstractDungeon.getCurrRoom()).monsters.monsters) {
            enemyPower(p, mo);
        }
    }

    protected void dmgAct(AbstractMonster m, int amount) {
        addToBot(
                new DamageAction(m, new DamageInfo(AbstractDungeon.player, amount, DamageInfo.DamageType.NORMAL)));
    }

    protected void dmgAct(AbstractMonster m, int amount, DamageType type) {
        addToBot(
                new DamageAction(m, new DamageInfo(AbstractDungeon.player, amount, type)));
    }

    protected void blkAct(int amount) {
        addToBot(new GainBlockAction(AbstractDungeon.player, amount));
    }

    protected void heal(int amount) {
        addToBot(new HealAction(AbstractDungeon.player, AbstractDungeon.player, amount));
    }

    protected void lose(int amount) {
        addToBot(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, amount));
    }

    protected AbstractCreature getUser() {
        return CardUtil.getCardSource();
    }

    protected AbstractPlayer getP() {
        return AbstractDungeon.player;
    }

    protected int countPower(String s) {
        int amount = 0;
        if (getP().hasPower(s)) {
            amount = getP().getPower(s).amount;
        }
        return amount;
    }

    public void downgrade() {
    }

    public boolean canDowngrade() {
        return false;
    }

    public boolean isCardInHand() {
        return AbstractDungeon.player.hand.group.contains(this);
    }

    public boolean isCardInDrawPile() {
        return AbstractDungeon.player.drawPile.group.contains(this);
    }

    public boolean isCardInDiscardPile() {
        return AbstractDungeon.player.discardPile.group.contains(this);
    }

    public boolean isCardInExhaustPile() {
        return AbstractDungeon.player.exhaustPile.group.contains(this);
    }

    public void updateFiend() {
        UpdateDescription();
        initializeDescription();
    }

    public void exitFiend() {
        UpdateDescription();
        initializeDescription();
    }

    public void updateManager() {
        UpdateDescription();
        initializeDescription();
    }

    public void exitManager() {
        UpdateDescription();
        initializeDescription();
    }

    public void updateNull() {
        UpdateDescription();
        initializeDescription();
    }

    @Override
    public abstract AbstractDoubleCard makeCopy();

    @Override
    public AbstractCard makeStatEquivalentCopy() {
        if (codexManagerPreview == null) return super.makeStatEquivalentCopy();
        return SS.helper.CardPreviewContext.withManager(codexManagerPreview, () -> super.makeStatEquivalentCopy());
    }

    public void refreshPower() {
    }

    public void triggerOnGlowCheck() {
        if (hasPairGlow()) {
            triggerOnPairGlow();
        } else {
            if (this.isFiend) {
                triggerOnGlowCheck_Fiend();
            }
            if (this.isManager) {
                triggerOnGlowCheck_Manager();
            }
        }
    }

    public void triggerOnGlowCheck_Fiend() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        if (AbstractDungeon.player.getPower("Double:FiendStance") != null) {
            this.glowColor = new Color(1.0F, 0.4F, 1.0F, 1.0F);
        }
    }

    public void triggerOnGlowCheck_Manager() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        if (AbstractDungeon.player.getPower("Double:ManagerStance") != null) {
            this.glowColor = AbstractCard.GOLD_BORDER_GLOW_COLOR.cpy();
        }
    }

    public void triggerOnPairGlow() {
        this.glowColor = AbstractCard.BLUE_BORDER_GLOW_COLOR.cpy();
        if (hasPairGlow()) {
            this.glowColor = new Color(1.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    public boolean hasPairGlow() {
        return false;
    }

    public static boolean isInCombat() {
        return CardCrawlGame.isInARun()
                && AbstractDungeon.currMapNode != null
                && AbstractDungeon.getCurrRoom() != null
                && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT
                && AbstractDungeon.player != null
                && !AbstractDungeon.player.isDead;
    }

    public void UpdateExhaustiveDescription() {
        if (this.purgeOnUse || this.exhaust)
            return;
        this.rawDescription = this.rawDescription.substring(0, this.rawDescription.length() - 1)
                + ExhaustiveField.ExhaustiveFields.exhaustive.get(this);
        initializeDescription();
    }

    public boolean needFiend() {
        if (codexManagerPreview != null) return false;
        return AbstractDungeon.player != null && AbstractDungeon.player.getPower("Double:FiendStance") != null
                && !(AbstractDungeon.getCurrRoom()).isBattleEnding();
    }

    public boolean needManager() {
        if (codexManagerPreview != null) return codexManagerPreview;
        return AbstractDungeon.player != null && AbstractDungeon.player.getPower("Double:ManagerStance") != null
                && !(AbstractDungeon.getCurrRoom()).isBattleEnding();
    }

    public void UpdateDescription() {
        UpdateDescription(0, 1, 2, 3, 4, 5);
    }

    /**
     * 取某个正式文案的简洁版替换。
     *
     * <p>简洁模式下 {@link SS.helper.ConciseText} 里有对应条目就用它，没有就原样返回。
     * 用值相等去查而不是直接把参数带进来：这个方法被 20 多处用不同的 index 调用，
     * 每处都要多传一遍 id 太吵，而 formal 本身（DESCRIPTION/UPGRADE_DESCRIPTION/
     * EXTENDED_DESCRIPTION[i]）已经唯一确定了要覆盖哪一句。
     */
    private String concise(String formal) {
        if (formal == null || !SS.helper.TextMode.isConcise()) {
            return formal;
        }
        // 先按 ED 槽位匹配：简洁表里的 ED 数组与正式 ED 数组按下标一一对应，
        // 所以这里用"formal 等于正式 ED 的第 i 项"来定位，而不是用 formal 去反查 map。
        String concised = matchExtended(formal);
        if (concised != null) {
            return concised;
        }
        String d = ConciseText.cardText(this.cardID, "DESCRIPTION");
        if (d != null && formal.equals(getFormalDescription(false))) {
            return d;
        }
        String u = ConciseText.cardText(this.cardID, "UPGRADE_DESCRIPTION");
        if (u != null && formal.equals(getFormalDescription(true))) {
            return u;
        }
        return formal;
    }

    /** 在简洁表的 ED 数组里找 formal 对应的那一项；没有则返回 null（由调用方继续往基础文案里找）。 */
    private String matchExtended(String formal) {
        String[] mine = ConciseText.cardExtended(this.cardID);
        if (mine == null) {
            return null;
        }
        int myLen = (EXTENDED_DESCRIPTION == null) ? 0 : EXTENDED_DESCRIPTION.length;
        // 只遍历两边都存在的下标，避免 formal==null 时误匹配到 null 槽位
        for (int i = 0; i < mine.length && i < myLen; i++) {
            String formalSlot = EXTENDED_DESCRIPTION[i];
            if (formalSlot != null && !formalSlot.isEmpty() && formalSlot.equals(formal)) {
                return mine[i];
            }
        }
        return null;
    }

    /** 正式（详细模式）文案：强化取 UPGRADE_DESCRIPTION，否则 DESCRIPTION。 */
    private String getFormalDescription(boolean upgraded) {
        if (CARD_STRINGS == null) {
            return null;
        }
        return upgraded ? CARD_STRINGS.UPGRADE_DESCRIPTION : CARD_STRINGS.DESCRIPTION;
    }

    public void UpdateDescription(int a, int b, int c, int d, int e, int f) {
        if (!isFiend && !isManager
                || (!isInCombat() && codexManagerPreview == null)) {
            this.rawDescription = concise(getFormalDescription(this.upgraded));
            return;
        }
        if (isFiend && !isManager) {
            if (this.upgraded) {
                if (needFiend()) {
                    this.rawDescription = concise(extended(b));
                } else {
                    this.rawDescription = concise(getFormalDescription(true));
                }
            } else {
                if (needFiend()) {
                    this.rawDescription = concise(extended(a));
                } else {
                    this.rawDescription = concise(getFormalDescription(false));
                }
            }
            return;
        }
        if (!isFiend && isManager) {
            if (this.upgraded) {
                if (needManager()) {
                    this.rawDescription = concise(extended(b));
                } else {
                    this.rawDescription = concise(getFormalDescription(true));
                }
            } else {
                if (needManager()) {
                    this.rawDescription = concise(extended(a));
                } else {
                    this.rawDescription = concise(getFormalDescription(false));
                }
            }
            return;
        }
        if (isFiend && isManager) {
            if (this.upgraded) {
                if (needFiend()) {
                    if (needManager()) {
                        this.rawDescription = concise(extended(f));
                    } else {
                        this.rawDescription = concise(extended(b));
                    }
                } else {
                    if (needManager()) {
                        this.rawDescription = concise(extended(d));
                    } else {
                        this.rawDescription = concise(getFormalDescription(true));
                    }
                }
            } else {
                if (needFiend()) {
                    if (needManager()) {
                        this.rawDescription = concise(extended(e));
                    } else {
                        this.rawDescription = concise(extended(a));
                    }
                } else {
                    if (needManager()) {
                        this.rawDescription = concise(extended(c));
                    } else {
                        this.rawDescription = concise(getFormalDescription(false));
                    }
                }
            }
        }
    }

    /**
     * 安全取 EXTENDED_DESCRIPTION 的第 i 项。
     *
     * <p>ED 数组长度由 JSON 决定，实测分布是 0 / 2 / 6 / 16 —— 大部分牌只有 2 项，
     * 而 fiend+manager 双身份会读到 index 4/5。老代码直接下标取值，靠"这些牌不会同时
     * 挂两种身份"侥幸不越界。这里补一层长度保护：越界返回 null，让 concise() 原样透传，
     * 再由调用方落到 rawDescription = null 之外的分支 —— 实际上越界时回退到基础文案更安全。
     */
    private String extended(int i) {
        if (EXTENDED_DESCRIPTION == null || i < 0 || i >= EXTENDED_DESCRIPTION.length) {
            return getFormalDescription(this.upgraded);
        }
        String s = EXTENDED_DESCRIPTION[i];
        // 空串是"该槽位没写"，同样回退
        return (s == null || s.isEmpty()) ? getFormalDescription(this.upgraded) : s;
    }
}
