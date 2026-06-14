package SS.interfaces;

import com.megacrit.cardcrawl.cards.AbstractCard;

public interface IEnvironmentCard {
    // 获取环境的唯一ID（未来用于匹配不同的背景特效）
    String getEnvironmentID();

    // 当环境生效（进场完毕）时触发的逻辑
    void onEnterEnvironment();

    // 当环境失效（被顶替或战斗结束）时触发的逻辑
    void onExitEnvironment();

    // 默认辅助方法：将实现该接口的对象安全地转换为原版 AbstractCard
    default AbstractCard asCard() {
        return (AbstractCard) this;
    }
}