package SS.helper;

import java.util.ArrayList;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.AbstractCard.CardColor;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

import SS.modcore.modcore;
import SS.packages.AbstractPackage;

public class SynergismGraph {
    public class Edge {
        public String u, v;// 起点，终点
        public SynTag w;

        public Edge() {
            u = "";
            v = "";
        }

        public Edge(String u, String v) {
            this.u = u;
            this.v = v;
        }

        public Edge(String u, String v, SynTag w) {
            this.u = u;
            this.v = v;
            this.w = w;
        }
    }

    public enum SynTag {
        Teacher, Student;
    }

    public static ArrayList<String> vert = new ArrayList<>();
    public static ArrayList<Edge> edge = new ArrayList<>();

    public SynergismGraph() {
        vert.clear();
        edge.clear();
    }

    public void add(String u, String v, SynTag w) {
        edge.add(new Edge(u, v, w));
    }

    public void add(CardColor u, CardColor v, SynTag w) {
        edge.add(new Edge(u.toString(), v.toString(), w));
    }

    public void add(AbstractPackage u, AbstractPackage v, SynTag w) {
        add(u.PackageColor, v.PackageColor, w);
    }

    public boolean hasSyn(String u, String v, SynTag w) {
        return edge.contains(new Edge(u, v, w));
    }

    public boolean hasSyn(CardColor u, CardColor v, SynTag w) {
        return edge.contains(new Edge(u.toString(), v.toString(), w));
    }

    public boolean hasSyn(AbstractPackage u, AbstractPackage v, SynTag w) {
        return hasSyn(u.PackageColor, v.PackageColor, w);
    }

    public ArrayList<SynTag> getAllSyn(String u, String v) {
        ArrayList<SynTag> temp = new ArrayList<>();
        for (Edge e : edge) {
            if (e.u.equals(u) && e.v.equals(v)) {
                temp.add(e.w);
            }
        }
        return temp;
    }

    public ArrayList<SynTag> getAllSyn(CardColor u, CardColor v) {
        return getAllSyn(u.toString(), v.toString());
    }

    public ArrayList<SynTag> getAllSyn(AbstractPackage u, AbstractPackage v) {
        return getAllSyn(u.PackageColor, v.PackageColor);
    }

    public boolean hasAnySyn(String u, SynTag w) {
        for (CardColor v : modcore.validColors) {
            if (hasSyn(u, v.toString(), w))
                return true;
        }
        return false;
    }

    public boolean hasAnySyn(CardColor u, SynTag w) {
        return hasAnySyn(u.toString(), w);
    }

    public boolean hasAnySynInGroup(CardColor u, SynTag w, CardGroup g) {
        for (AbstractCard c : g.group) {
            if (hasSyn(u, c.color, w))
                return true;
        }
        return false;
    }

    public boolean hasAnySynInHand(CardColor u, SynTag w, AbstractPlayer p) {
        return hasAnySynInGroup(u, w, p.hand);
    }

    public boolean hasAnySynInDeck(CardColor u, SynTag w, AbstractPlayer p) {
        return hasAnySynInGroup(u, w, p.masterDeck);
    }
}
