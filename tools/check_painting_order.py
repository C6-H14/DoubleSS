"""Headless queue regression checks against the production PaintingPower source.

Small engine stubs isolate callback order without launching Slay the Spire.
Run with Python 3 and a JDK on PATH; no installed Python packages required.
"""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    with tempfile.TemporaryDirectory(prefix="painting-check-") as temp:
        root = Path(temp)
        sources = []

        def stub(package, name, body):
            path = root / Path(*package.split(".")) / (name + ".java")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("package " + package + ";\n" + body, encoding="utf-8")
            sources.append(str(path))

        stub("com.megacrit.cardcrawl.actions", "AbstractGameAction", """
public abstract class AbstractGameAction {
 public static java.util.Deque<AbstractGameAction> queue = new java.util.ArrayDeque<>();
 public boolean isDone; public abstract void update();
}""")
        stub("com.megacrit.cardcrawl.actions.utility", "UseCardAction", "public class UseCardAction {}")
        stub("com.megacrit.cardcrawl.core", "AbstractCreature", """
public class AbstractCreature {
 public java.util.Map<String,com.megacrit.cardcrawl.powers.AbstractPower> powers=new java.util.HashMap<>();
 public com.megacrit.cardcrawl.powers.AbstractPower getPower(String id){return powers.get(id);}
}""")
        stub("com.megacrit.cardcrawl.monsters", "AbstractMonster", """
public class AbstractMonster extends com.megacrit.cardcrawl.core.AbstractCreature {
 public boolean dead; public boolean isDeadOrEscaped(){return dead;}
}""")
        stub("com.megacrit.cardcrawl.cards", "AbstractCard", """
public class AbstractCard {
 public enum CardType {ATTACK,SKILL,POWER,STATUS,CURSE}
 public CardType type=CardType.ATTACK;
}""")
        stub("com.megacrit.cardcrawl.localization", "PowerStrings", """
public class PowerStrings { public String NAME="Painting"; public String[] DESCRIPTIONS={"", ""}; }
""")
        stub("com.megacrit.cardcrawl.core", "CardCrawlGame", """
public class CardCrawlGame {
 public static LanguagePack languagePack=new LanguagePack();
 public static class LanguagePack {
  public com.megacrit.cardcrawl.localization.PowerStrings getPowerStrings(String id){
   return new com.megacrit.cardcrawl.localization.PowerStrings();
  }
 }
}""")
        stub("com.badlogic.gdx.graphics.g2d", "TextureAtlas", """
public class TextureAtlas {public static class AtlasRegion {public AtlasRegion(Object o,int a,int b,int c,int d){}}}
""")
        stub("com.megacrit.cardcrawl.helpers", "ImageMaster", "public class ImageMaster {public static Object loadImage(String s){return null;}}")
        stub("SS.helper", "ModHelper", 'public class ModHelper {public static String makePath(String s){return "Double:"+s;}}')
        stub("SS.monster.ally", "AllyManager", """
public class AllyManager {public static Group allies=new Group(); public static class Group {
 public java.util.ArrayList<com.megacrit.cardcrawl.monsters.AbstractMonster> monsters=new java.util.ArrayList<>();
}}""")
        stub("SS.monster.ally", "SoulAlly", """
public class SoulAlly extends com.megacrit.cardcrawl.monsters.AbstractMonster {
 public enum SoulColor {WHITE,RED,GREEN,BLUE,PURPLE}
 public SoulColor stateColor=SoulColor.WHITE; public int changes;
 public void changeColor(SoulColor color){stateColor=color;changes++;}
}""")
        stub("com.megacrit.cardcrawl.dungeons", "AbstractDungeon", """
public class AbstractDungeon {
 public static Player player=new Player();
 public static class Player extends com.megacrit.cardcrawl.core.AbstractCreature {public Group discardPile=new Group();}
 public static class Group {public java.util.ArrayList<com.megacrit.cardcrawl.cards.AbstractCard> group=new java.util.ArrayList<>();}
}""")
        stub("com.megacrit.cardcrawl.powers", "AbstractPower", """
public class AbstractPower {
 public enum PowerType {BUFF,DEBUFF}
 public String name,ID,description; public int amount; public PowerType type;
 public com.megacrit.cardcrawl.core.AbstractCreature owner; public Object region128,region48;
 public void flash(){} public void updateDescription(){}
 public void onUseCard(com.megacrit.cardcrawl.cards.AbstractCard c,com.megacrit.cardcrawl.actions.utility.UseCardAction a){}
 public void onAfterUseCard(com.megacrit.cardcrawl.cards.AbstractCard c,com.megacrit.cardcrawl.actions.utility.UseCardAction a){}
 protected void addToBot(com.megacrit.cardcrawl.actions.AbstractGameAction a){com.megacrit.cardcrawl.actions.AbstractGameAction.queue.addLast(a);}
 protected void addToTop(com.megacrit.cardcrawl.actions.AbstractGameAction a){com.megacrit.cardcrawl.actions.AbstractGameAction.queue.addFirst(a);}
}""")
        stub("com.megacrit.cardcrawl.actions.common", "ReducePowerAction", """
public class ReducePowerAction extends com.megacrit.cardcrawl.actions.AbstractGameAction {
 private com.megacrit.cardcrawl.powers.AbstractPower power; private int amount;
 public ReducePowerAction(com.megacrit.cardcrawl.core.AbstractCreature a,com.megacrit.cardcrawl.core.AbstractCreature b,
   com.megacrit.cardcrawl.powers.AbstractPower p,int n){power=p;amount=n;}
 public void update(){power.amount-=amount;isDone=true;}
}""")
        stub("checks", "PaintingOrderCheck", """
import SS.power.PaintingPower;
import SS.monster.ally.*;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
public class PaintingOrderCheck {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void drain(){int n=0;while(!AbstractGameAction.queue.isEmpty()){
  if(++n>100)throw new AssertionError("queue loop");AbstractGameAction.queue.removeFirst().update();}}
 static AbstractGameAction run(Runnable r){return new AbstractGameAction(){public void update(){r.run();isDone=true;}};}
 static PaintingPower power(int n){
  AbstractGameAction.queue.clear(); AllyManager.allies.monsters.clear();
  AbstractDungeon.player.powers.clear();AbstractDungeon.player.discardPile.group.clear();
  PaintingPower p=new PaintingPower(AbstractDungeon.player,n);AbstractDungeon.player.powers.put(p.ID,p);return p;
 }
 public static void main(String[] args){
  AbstractCard card=new AbstractCard(); PaintingPower p=power(2);
  SoulAlly soul=new SoulAlly();AllyManager.allies.monsters.add(soul);
  UseCardAction use=new UseCardAction();p.onUseCard(card,use);
  check(soul.changes==0,"onUseCard must not recolor immediately");
  StringBuilder effects=new StringBuilder();
  final SoulAlly initialSoul=soul; final UseCardAction initialUse=use;
  AbstractGameAction.queue.add(run(()->{effects.append("card");
   AbstractGameAction.queue.add(run(()->{effects.append("/child");initialSoul.stateColor=SoulAlly.SoulColor.GREEN;}));}));
  final PaintingPower first=p; AbstractGameAction.queue.add(run(()->first.onAfterUseCard(card,initialUse)));
  drain();check(effects.toString().equals("card/child"),"nested card effects ran");
  check(soul.stateColor==SoulAlly.SoulColor.RED && p.amount==1,"painting must follow nested effects");
  p.onAfterUseCard(card,use);drain();check(p.amount==1,"duplicate callback must not spend twice");
  p=power(1);p.onAfterUseCard(card,new UseCardAction());drain();
  check(p.amount==1,"painting gained by current play must not trigger retroactively");
  p=power(1);use=new UseCardAction();p.onUseCard(card,use);
  soul=new SoulAlly();AllyManager.allies.monsters.add(soul);p.onAfterUseCard(card,use);drain();
  check(soul.changes==1 && p.amount==0,"soul summoned by card is eligible after resolution");
  p=power(1);SoulAlly dead=new SoulAlly();dead.dead=true;soul=new SoulAlly();
  AllyManager.allies.monsters.add(dead);AllyManager.allies.monsters.add(soul);
  use=new UseCardAction();p.onUseCard(card,use);p.onAfterUseCard(card,use);drain();
  check(dead.changes==0 && soul.changes==1,"dead souls must be excluded");
  p=power(1);use=new UseCardAction();p.onUseCard(card,use);p.onAfterUseCard(card,use);drain();
  check(p.amount==1,"no living soul must not consume a charge");
  p=power(1);soul=new SoulAlly();AllyManager.allies.monsters.add(soul);
  UseCardAction a=new UseCardAction(),b=new UseCardAction();p.onUseCard(card,a);p.onUseCard(card,b);
  p.onAfterUseCard(card,a);p.onAfterUseCard(card,b);drain();
  check(soul.changes==1 && p.amount==0,"pending plays must not overdraw painting charges");
  System.out.println("PaintingPower: 7 queue/order/charge regression scenarios passed.");
 }
}""")
        sources.extend(str(ROOT / "src/main/java" / p) for p in (
            "SS/power/PaintingPower.java", "SS/interfaces/OnPaintingSubscriber.java"))
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(root / "classes"), *sources], check=True)
        subprocess.run(["java", "-cp", str(root / "classes"), "checks.PaintingOrderCheck"], check=True)


if __name__ == "__main__":
    main()
