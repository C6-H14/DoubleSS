package SS.rewards;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.rewards.RewardItem;

public class RewardManager {
    public ArrayList<RewardItem> rewards = new ArrayList<>();
    public RewardItem commomCardReward;
    public CommonCardReward ccardrng = new CommonCardReward();

    public RewardManager() {
        rewards.clear();
    }

    public void update() {
        rewards.clear();
        ccardrng.update();
    }

    public void addCardReward(int amount) {
        RewardItem cardReward;
        System.out.println("add card rewards");
        for (int i = 0; i < amount; ++i) {
            cardReward = new RewardItem();
            cardReward.cards.clear();
            cardReward.cards.addAll(CommonCardReward.getRewardCards());
            for (AbstractCard c : cardReward.cards) {
                System.out.print(c.cardID + ":" + c.upgraded + " ");
            }
            System.out.println("");
            if (cardReward.cards.size() > 0) {
                this.rewards.add(cardReward);
            }
        }
        System.out.println(rewards.size());
    }

}
