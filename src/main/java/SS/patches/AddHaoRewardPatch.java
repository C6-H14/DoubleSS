package SS.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import SS.modcore.modcore;
import SS.rewards.HaoReward;

public class AddHaoRewardPatch {
    @SpirePatch(clz = AbstractRoom.class, method = "addPotionToRewards", paramtypez = {})
    public static class DropSeals {
        public static void Postfix(AbstractRoom __instance) {
            if (AbstractDungeon.player instanceof SS.characters.MyCharacter) {
                int chance = 10;
                if (__instance instanceof com.megacrit.cardcrawl.rooms.MonsterRoomElite) {
                    chance = 25;
                } else if (__instance instanceof com.megacrit.cardcrawl.rooms.MonsterRoomBoss) {
                    chance = 45;
                }

                chance += (int) (modcore.Hao_chance * 5);

                if (__instance.rewards.size() >= 5) {
                    chance = 0;
                }

                if (AbstractDungeon.potionRng.random(0, 99) <= chance)
                    __instance.rewards.add(new HaoReward());
            }
        }
    }
}