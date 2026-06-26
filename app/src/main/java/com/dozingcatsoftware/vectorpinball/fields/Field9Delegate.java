package com.dozingcatsoftware.vectorpinball.fields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.dozingcatsoftware.vectorpinball.elements.DropTargetGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.FieldElement;
import com.dozingcatsoftware.vectorpinball.elements.RolloverGroupElement;
import com.dozingcatsoftware.vectorpinball.elements.SensorElement;
import com.dozingcatsoftware.vectorpinball.elements.WallElement;
import com.dozingcatsoftware.vectorpinball.model.Ball;
import com.dozingcatsoftware.vectorpinball.model.BaseFieldDelegate;
import com.dozingcatsoftware.vectorpinball.model.Color;
import com.dozingcatsoftware.vectorpinball.model.Field;
import com.dozingcatsoftware.vectorpinball.model.Shape;

/**
 * Delegate for the "Royal Flush" poker table (table9). The player builds a five-card poker hand by
 * shooting the left and right ramps, each of which displays the card that will be collected. Drop
 * target banks on each ramp cycle that ramp's card; completing a bank gives a "lucky" card chosen to
 * improve the current hand. The center ramp swaps both ramp cards for a lucky pair. Completing a
 * five-card hand scores based on poker rank, and three-of-a-kind or better starts multiball, during
 * which any ramp scores a jackpot of the hand value plus accumulated hand bonus.
 */
public class Field9Delegate extends BaseFieldDelegate {

    static final class Card {
        final int rank;
        final String suit;

        Card(int rank, String suit) {
            this.rank = rank;
            this.suit = suit;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Card)) return false;
            Card other = (Card) o;
            return rank == other.rank && suit.equals(other.suit);
        }

        @Override public int hashCode() {
            return 31 * rank + suit.hashCode();
        }

        @Override public String toString() {
            return rank + suit;
        }
    }

    static final List<Integer> ALL_RANKS =
            Collections.unmodifiableList(Arrays.asList(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14));
    static final List<String> ALL_SUITS =
            Collections.unmodifiableList(Arrays.asList("S", "H", "D", "C"));

    static final List<Card> ALL_CARDS = buildAllCards();

    private static List<Card> buildAllCards() {
        List<Card> cards = new ArrayList<>();
        for (int rank : ALL_RANKS) {
            for (String suit : ALL_SUITS) {
                cards.add(new Card(rank, suit));
            }
        }
        return Collections.unmodifiableList(cards);
    }

    static final Map<String, Integer> SUIT_COLORS = new HashMap<>();
    static final Map<String, String> SUIT_NAMES = new HashMap<>();
    static final Map<Integer, String> RANK_NAMES = new HashMap<>();
    static {
        SUIT_COLORS.put("H", Color.fromRGB(235, 60, 70));
        SUIT_COLORS.put("D", Color.fromRGB(90, 140, 255));
        SUIT_COLORS.put("C", Color.fromRGB(80, 210, 100));
        SUIT_COLORS.put("S", Color.fromRGB(225, 225, 235));

        SUIT_NAMES.put("H", "Hearts");
        SUIT_NAMES.put("D", "Diamonds");
        SUIT_NAMES.put("C", "Clubs");
        SUIT_NAMES.put("S", "Spades");

        for (int r = 2; r <= 10; r++) RANK_NAMES.put(r, String.valueOf(r));
        RANK_NAMES.put(11, "Jack");
        RANK_NAMES.put(12, "Queen");
        RANK_NAMES.put(13, "King");
        RANK_NAMES.put(14, "Ace");
    }

    /*
     14-segment display endpoints in a unit box (x in -0.5..0.5, y in -0.8..0.8).
     ====== a ======
     |      |    //|
     f      i  /j  b
     |      |//    |
     == g1 === g2 ==
     |      |\\    |
     e      l  \m  c
     |      |    \\|
     ====== d ======
     */
    static final Map<String, double[]> SEGMENTS = new HashMap<>();
    static {
        SEGMENTS.put("a", new double[] {-0.44, 0.8, 0.44, 0.8});
        SEGMENTS.put("b", new double[] {0.5, 0.74, 0.5, 0.06});
        SEGMENTS.put("c", new double[] {0.5, -0.06, 0.5, -0.74});
        SEGMENTS.put("d", new double[] {-0.44, -0.8, 0.44, -0.8});
        SEGMENTS.put("e", new double[] {-0.5, -0.74, -0.5, -0.06});
        SEGMENTS.put("f", new double[] {-0.5, 0.06, -0.5, 0.74});
        SEGMENTS.put("g1", new double[] {-0.44, 0, -0.06, 0});
        SEGMENTS.put("g2", new double[] {0.06, 0, 0.44, 0});
        SEGMENTS.put("i", new double[] {0, 0.74, 0, 0.06});
        SEGMENTS.put("j", new double[] {0.4, 0.7, 0.1, 0.1});
        SEGMENTS.put("l", new double[] {0, -0.74, 0, -0.06});
        SEGMENTS.put("m", new double[] {0.4, -0.7, 0.1, -0.1});
    }

    static final Map<Integer, String[]> RANK_SEGMENTS = new HashMap<>();
    static {
        RANK_SEGMENTS.put(2, new String[] {"a", "b", "g1", "g2", "e", "d"});
        RANK_SEGMENTS.put(3, new String[] {"a", "b", "g1", "g2", "c", "d"});
        RANK_SEGMENTS.put(4, new String[] {"f", "g1", "g2", "b", "c"});
        RANK_SEGMENTS.put(5, new String[] {"a", "f", "g1", "g2", "c", "d"});
        RANK_SEGMENTS.put(6, new String[] {"a", "f", "g1", "g2", "c", "d", "e"});
        RANK_SEGMENTS.put(7, new String[] {"a", "b", "c"});
        RANK_SEGMENTS.put(8, new String[] {"a", "b", "c", "d", "e", "f", "g1", "g2"});
        RANK_SEGMENTS.put(9, new String[] {"a", "b", "c", "d", "f", "g1", "g2"});
        RANK_SEGMENTS.put(10, new String[] {"a", "i", "l"});
        RANK_SEGMENTS.put(11, new String[] {"b", "c", "d", "e"});
        RANK_SEGMENTS.put(12, new String[] {"a", "b", "c", "d", "e", "f", "m"});
        RANK_SEGMENTS.put(13, new String[] {"f", "e", "g1", "j", "m"});
        RANK_SEGMENTS.put(14, new String[] {"a", "b", "c", "e", "f", "g1", "g2"});
    }

    // ---- Hand display layout ----

    static final double[] HAND_CARD_XS = {5.9, 7.65, 9.4, 11.15, 12.9};
    static final double HAND_CARD_Y = 11;
    static final double HAND_CARD_SIZE = 1.5;

    static final double[] LEFT_RAMP_CARD_POS = {2.8, 17.5};
    static final double LEFT_RAMP_CARD_SIZE = 1.6;
    static final double[] RIGHT_RAMP_CARD_POS = {16.5, 16.6};
    static final double RIGHT_RAMP_CARD_SIZE = 1.6;

    static final long CARD_COLLECT_SCORE = 2500;
    static final long CENTER_RAMP_SCORE = 2500;

    // ---- Game state ----

    enum MultiballStatus {INACTIVE, STARTING, ACTIVE}

    final Random random = new Random();
    final List<Card> hand = new ArrayList<>();
    long handBonus = 0;
    MultiballStatus multiballStatus = MultiballStatus.INACTIVE;
    Card leftRampCard;
    Card rightRampCard;
    WallElement launchBarrier;

    // ---- Card drawing ----

    private List<Card> drawRandomCards(int n, Collection<Card> usedCards) {
        List<Card> deck = new ArrayList<>(ALL_CARDS);
        Collections.shuffle(deck, random);
        deck.removeAll(usedCards);
        return new ArrayList<>(deck.subList(0, n));
    }

    private Card drawRandomCard(Collection<Card> usedCards) {
        return drawRandomCards(1, usedCards).get(0);
    }

    // ---- Poker helpers ----

    /**
     * Given a partial set of ranks, returns a rank that would extend them toward a straight, or null
     * if no single card can. Used to pick "lucky" cards. The input may have 1-4 ranks.
     */
    static Integer bestRankForStraight(List<Integer> unsortedRanks) {
        List<Integer> ranks = new ArrayList<>(unsortedRanks);
        ranks.sort(Collections.reverseOrder());
        if (new LinkedHashSet<>(ranks).size() < ranks.size()) {
            // One or more pairs, no straight possible.
            return null;
        }
        if (ranks.size() == 1) {
            // JT has better straight possibilities than QJ.
            int r = ranks.get(0);
            return r >= 11 ? r - 1 : r + 1;
        }
        // Check for wheel (A-5 straight).
        boolean allWheelOrAce = true;
        for (int r : ranks) {
            if (!(r == 14 || r <= 5)) {
                allWheelOrAce = false;
                break;
            }
        }
        if (ranks.contains(14) && allWheelOrAce) {
            for (int candidate : new int[] {14, 5, 4, 3, 2}) {
                if (!ranks.contains(candidate)) return candidate;
            }
        }
        int max = ranks.get(0);
        int min = ranks.get(ranks.size() - 1);
        int diff = max - min;
        if (diff > 4) {
            return null;
        }
        if (diff == ranks.size() - 1) {
            // No gaps, e.g. 987.
            return max == 14 ? min - 1 : max + 1;
        }
        else {
            // Use highest gap.
            for (int v = max; v >= min; v--) {
                if (!ranks.contains(v)) return v;
            }
            return null;
        }
    }

    /** Maps group size to the list of ranks appearing that many times. */
    static Map<Integer, List<Integer>> groupSizesToRanks(List<Integer> ranks) {
        // [8, 8, 7, 6, 6] -> {2: [8, 6], 1: [7]}
        Map<Integer, List<Integer>> groupSizes = new LinkedHashMap<>();
        for (int r : new LinkedHashSet<>(ranks)) {
            int size = Collections.frequency(ranks, r);
            groupSizes.computeIfAbsent(size, k -> new ArrayList<>()).add(r);
        }
        return groupSizes;
    }

    private static List<Integer> ranksDescending(List<Card> h) {
        List<Integer> ranks = new ArrayList<>();
        for (Card c : h) ranks.add(c.rank);
        ranks.sort(Collections.reverseOrder());
        return ranks;
    }

    private static String firstSuitOtherThan(List<String> suits, String exclude) {
        for (String s : suits) {
            if (!s.equals(exclude)) return s;
        }
        return null;
    }

    private static List<String> suitsOfRank(List<Card> h, int rank) {
        List<String> out = new ArrayList<>();
        for (Card c : h) {
            if (c.rank == rank) out.add(c.suit);
        }
        return out;
    }

    private static boolean allSameSuit(List<Card> h) {
        String suit = h.get(0).suit;
        for (Card c : h) {
            if (!c.suit.equals(suit)) return false;
        }
        return true;
    }

    private static <T> List<T> minus(List<T> base, Collection<T> remove) {
        List<T> result = new ArrayList<>(base);
        result.removeAll(remove);
        return result;
    }

    /**
     * Chooses two "lucky" cards (left ramp, right ramp) that improve the current hand as much as
     * possible. The hand must have 1-4 cards.
     */
    Card[] drawLuckyCards(List<Card> h) {
        Card left = null;
        Card right = null;

        List<String> randSuits = new ArrayList<>(ALL_SUITS);
        Collections.shuffle(randSuits, random);
        List<Integer> handRanks = ranksDescending(h);

        if (h.size() == 1) {
            // Left for pair, right to build a straight flush.
            left = new Card(h.get(0).rank, firstSuitOtherThan(randSuits, h.get(0).suit));
            int rightRank = h.get(0).rank > 10 ? h.get(0).rank - 1 : h.get(0).rank + 1;
            right = new Card(rightRank, h.get(0).suit);
        }
        else if (h.size() == 2) {
            List<String> remainingSuits = minus(randSuits, suitsOf(h));
            if (h.get(0).rank == h.get(1).rank) {
                // Both make three of a kind.
                left = new Card(handRanks.get(0), remainingSuits.get(0));
                right = new Card(handRanks.get(0), remainingSuits.get(1));
            }
            else {
                // Left makes a pair, right makes a straight/flush if possible, else the other pair.
                left = new Card(handRanks.get(0), remainingSuits.get(0));
                Integer straightRank = bestRankForStraight(handRanks);
                right = (straightRank != null)
                        ? new Card(straightRank, h.get(0).suit)
                        : new Card(handRanks.get(1), remainingSuits.get(1));
            }
        }
        else if (h.size() == 3) {
            Map<Integer, List<Integer>> groups = groupSizesToRanks(handRanks);
            if (groups.get(3) != null) {
                String remainingSuit = minus(randSuits, suitsOf(h)).get(0);
                left = right = new Card(h.get(0).rank, remainingSuit);
            }
            else if (groups.get(2) != null) {
                List<String> remainingSuits = minus(randSuits, suitsOfRank(h, groups.get(2).get(0)));
                left = new Card(groups.get(2).get(0), remainingSuits.get(0));
                right = new Card(groups.get(2).get(0), remainingSuits.get(1));
            }
            else {
                // No pair: left matches highest card, right makes a straight if possible.
                List<String> remainingSuits = minus(randSuits, suitsOfRank(h, groups.get(1).get(0)));
                Integer straightRank = bestRankForStraight(handRanks);
                left = new Card(groups.get(1).get(0), remainingSuits.get(0));
                right = (straightRank != null)
                        ? new Card(straightRank, h.get(0).suit)
                        : new Card(groups.get(1).get(0), remainingSuits.get(1));
            }
        }
        else if (h.size() == 4) {
            Map<Integer, List<Integer>> groups = groupSizesToRanks(handRanks);
            if (groups.get(4) != null) {
                // Already have four of a kind; the last card is irrelevant.
                int r = h.get(0).rank == 14 ? 13 : 14;
                left = new Card(r, randSuits.get(0));
                right = new Card(r, randSuits.get(1));
            }
            else if (groups.get(3) != null) {
                String remainingSuit = minus(randSuits, suitsOf(h)).get(0);
                left = right = new Card(groups.get(3).get(0), remainingSuit);
            }
            else if (groups.get(2) != null) {
                List<String> remainingSuits = minus(randSuits, suitsOfRank(h, groups.get(2).get(0)));
                left = new Card(groups.get(2).get(0), remainingSuits.get(0));
                right = new Card(groups.get(2).get(0), remainingSuits.get(1));
            }
            else {
                List<String> remainingSuits = minus(randSuits, suitsOfRank(h, groups.get(1).get(0)));
                Integer straightRank = bestRankForStraight(handRanks);
                if (straightRank != null) {
                    left = new Card(straightRank, h.get(0).suit);
                    // If left is above the top card in hand, we're open-ended (could be a wheel
                    // if 5432).
                    if (straightRank > handRanks.get(0)) {
                        int bottom = handRanks.get(handRanks.size() - 1) == 2
                                ? 14
                                : handRanks.get(handRanks.size() - 1) - 1;
                        right = new Card(bottom, h.get(0).suit);
                    }
                    else {
                        right = new Card(straightRank, h.get(0).suit);
                    }
                }
                else if (allSameSuit(h)) {
                    // Possible flush.
                    List<Integer> remainingRanks = minus(ALL_RANKS, handRanks);
                    remainingRanks.sort(Collections.reverseOrder());
                    left = new Card(remainingRanks.get(0), h.get(0).suit);
                    right = new Card(remainingRanks.get(1), h.get(0).suit);
                }
                else {
                    left = new Card(groups.get(1).get(0), remainingSuits.get(0));
                    right = new Card(groups.get(1).get(0), remainingSuits.get(1));
                }
            }
        }
        return new Card[] {left, right};
    }

    private static List<String> suitsOf(List<Card> h) {
        List<String> suits = new ArrayList<>();
        for (Card c : h) suits.add(c.suit);
        return suits;
    }

    static final class HandResult {
        final String name;
        final long bonus;
        final boolean multiball;

        HandResult(String name, long bonus, boolean multiball) {
            this.name = name;
            this.bonus = bonus;
            this.multiball = multiball;
        }
    }

    /** Evaluates the current five-card hand. */
    HandResult evaluateHand() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card c : hand) {
            counts.merge(c.rank, 1, Integer::sum);
        }
        List<Integer> groups = new ArrayList<>(counts.values());
        groups.sort(Collections.reverseOrder());
        boolean isFlush = allSameSuit(hand);
        List<Integer> ranks = new ArrayList<>();
        for (Card c : hand) ranks.add(c.rank);
        Collections.sort(ranks);
        // A2345 is a straight.
        boolean isStraight = groups.get(0) == 1
                && (ranks.get(0) + 4 == ranks.get(4) || ranks.equals(Arrays.asList(2, 3, 4, 5, 14)));

        if (isFlush && ranks.equals(Arrays.asList(10, 11, 12, 13, 14))) {
            return new HandResult("Royal Flush", 100000, true);
        }
        if (isStraight && isFlush) return new HandResult("Straight Flush", 75000, true);
        if (groups.get(0) == 4) return new HandResult("Four of a Kind", 60000, true);
        if (groups.get(0) == 3 && groups.get(1) == 2) return new HandResult("Full House", 50000, true);
        if (isFlush) return new HandResult("Flush", 40000, true);
        if (isStraight) return new HandResult("Straight", 35000, true);
        if (groups.get(0) == 3) return new HandResult("Three of a Kind", 30000, true);
        if (groups.get(0) == 2 && groups.get(1) == 2) return new HandResult("Two Pair", 20000, false);
        if (groups.get(0) == 2) return new HandResult("Pair", 10000, false);
        return new HandResult("High Card", 5000, false);
    }

    // ---- Card / hand graphics ----

    /** Appends shapes for one suit glyph centered at (gx, gy) with size s. */
    private void addSuitShapes(List<Shape> shapes, String suit, double gx, double gy, double s,
            int color) {
        if (suit.equals("H")) {
            shapes.add(Shape.Arc.create(gx - 0.23 * s, gy + 0.17 * s, 0.25 * s, 0.25 * s, 0, 180, 0, color, null));
            shapes.add(Shape.Arc.create(gx + 0.23 * s, gy + 0.17 * s, 0.25 * s, 0.25 * s, 0, 180, 0, color, null));
            shapes.add(Shape.Line.create(gx - 0.48 * s, gy + 0.13 * s, gx, gy - 0.5 * s, 0, color, null));
            shapes.add(Shape.Line.create(gx + 0.48 * s, gy + 0.13 * s, gx, gy - 0.5 * s, 0, color, null));
        }
        else if (suit.equals("D")) {
            shapes.add(Shape.Line.create(gx, gy + 0.5 * s, gx + 0.36 * s, gy, 0, color, null));
            shapes.add(Shape.Line.create(gx + 0.36 * s, gy, gx, gy - 0.5 * s, 0, color, null));
            shapes.add(Shape.Line.create(gx, gy - 0.5 * s, gx - 0.36 * s, gy, 0, color, null));
            shapes.add(Shape.Line.create(gx - 0.36 * s, gy, gx, gy + 0.5 * s, 0, color, null));
        }
        else if (suit.equals("C")) {
            shapes.add(Shape.Circle.create(gx, gy + 0.26 * s, 0.21 * s, Shape.FillType.SOLID, 0, color, null));
            shapes.add(Shape.Circle.create(gx - 0.23 * s, gy - 0.06 * s, 0.21 * s, Shape.FillType.SOLID, 0, color, null));
            shapes.add(Shape.Circle.create(gx + 0.23 * s, gy - 0.06 * s, 0.21 * s, Shape.FillType.SOLID, 0, color, null));
            shapes.add(Shape.Line.create(gx, gy - 0.1 * s, gx, gy - 0.5 * s, 0, color, null));
        }
        else {
            shapes.add(Shape.Line.create(gx, gy + 0.5 * s, gx - 0.4 * s, gy - 0.08 * s, 0, color, null));
            shapes.add(Shape.Line.create(gx, gy + 0.5 * s, gx + 0.4 * s, gy - 0.08 * s, 0, color, null));
            shapes.add(Shape.Arc.create(gx - 0.2 * s, gy - 0.14 * s, 0.2 * s, 0.2 * s, 180, 360, 0, color, null));
            shapes.add(Shape.Arc.create(gx + 0.2 * s, gy - 0.14 * s, 0.2 * s, 0.2 * s, 180, 360, 0, color, null));
            shapes.add(Shape.Line.create(gx, gy - 0.16 * s, gx, gy - 0.5 * s, 0, color, null));
        }
    }

    /**
     * Appends shapes for one card of width w centered at (cx, cy). A null card draws an empty slot
     * outline.
     */
    private void addCardShapes(List<Shape> shapes, double cx, double cy, double w, Card card) {
        double hw = 0.5 * w;
        double hh = 0.7 * w;
        int border = (card != null) ? Color.fromRGB(210, 210, 220) : Color.fromRGB(70, 70, 85);
        shapes.add(Shape.Line.create(cx - hw, cy - hh, cx - hw, cy + hh, 0, border, null));
        shapes.add(Shape.Line.create(cx - hw, cy + hh, cx + hw, cy + hh, 0, border, null));
        shapes.add(Shape.Line.create(cx + hw, cy + hh, cx + hw, cy - hh, 0, border, null));
        shapes.add(Shape.Line.create(cx + hw, cy - hh, cx - hw, cy - hh, 0, border, null));
        if (card != null) {
            int color = SUIT_COLORS.get(card.suit);
            double ds = 0.42 * w;
            double dy = cy + 0.27 * w;
            for (String name : RANK_SEGMENTS.get(card.rank)) {
                double[] u = SEGMENTS.get(name);
                shapes.add(Shape.Line.create(
                        cx + u[0] * ds, dy + u[1] * ds, cx + u[2] * ds, dy + u[3] * ds, 0, color, null));
            }
            addSuitShapes(shapes, card.suit, cx, cy - 0.38 * w, 0.55 * w, color);
        }
    }

    private void rebuildShapes(Field field) {
        List<Shape> shapes = new ArrayList<>();
        if (leftRampCard != null) {
            addCardShapes(shapes, LEFT_RAMP_CARD_POS[0], LEFT_RAMP_CARD_POS[1], LEFT_RAMP_CARD_SIZE,
                    leftRampCard);
        }
        if (rightRampCard != null) {
            addCardShapes(shapes, RIGHT_RAMP_CARD_POS[0], RIGHT_RAMP_CARD_POS[1], RIGHT_RAMP_CARD_SIZE,
                    rightRampCard);
        }
        for (int i = 0; i < 5; i++) {
            addCardShapes(shapes, HAND_CARD_XS[i], HAND_CARD_Y, HAND_CARD_SIZE,
                    (i < hand.size()) ? hand.get(i) : null);
        }
        field.setShapes(shapes);
    }

    // ---- Multiball ----

    private void startMultiball(final Field field) {
        multiballStatus = MultiballStatus.STARTING;
        Runnable launchBall = () -> {
            if (field.getBalls().size() < 3) field.launchBall();
        };
        field.scheduleAction(1000, launchBall);
        field.scheduleAction(3500, () -> {
            launchBall.run();
            multiballStatus = MultiballStatus.ACTIVE;
        });
    }

    private void scoreMultiballJackpot(Field field, Ball ball) {
        long jackpot = handBonus + evaluateHand().bonus;
        field.addScoreWithAnimation(jackpot, ball.getPosition());
        field.showGameMessage(field.resolveString("jackpot_received_message"), 1500);
    }

    private void clearMultiballStatus(Field field) {
        multiballStatus = MultiballStatus.INACTIVE;
        hand.clear();
        List<Card> cards = drawRandomCards(2, Collections.<Card>emptyList());
        leftRampCard = cards.get(0);
        rightRampCard = cards.get(1);
        rebuildShapes(field);
    }

    // ---- Card collection / ramp handlers ----

    private void collectCard(Field field, Ball ball, Card card) {
        hand.add(card);
        if (hand.size() < 5) {
            field.showGameMessage(
                    RANK_NAMES.get(card.rank) + " of " + SUIT_NAMES.get(card.suit), 1500);
            field.addScoreWithAnimation(CARD_COLLECT_SCORE, ball.getPosition());

            if (card.equals(leftRampCard)) {
                List<Card> used = new ArrayList<>(hand);
                used.add(rightRampCard);
                leftRampCard = drawRandomCard(used);
            }
            if (card.equals(rightRampCard)) {
                List<Card> used = new ArrayList<>(hand);
                used.add(leftRampCard);
                rightRampCard = drawRandomCard(used);
            }
        }
        else {
            HandResult result = evaluateHand();
            long jackpot = result.bonus + handBonus;
            field.addScoreWithAnimation(jackpot, ball.getPosition());
            field.showGameMessage(
                    field.resolveString("poker_hand_scored_message", result.name, jackpot), 2500);
            if (result.multiball) {
                startMultiball(field);
                leftRampCard = rightRampCard = null;
            }
            else {
                hand.clear();
                List<Card> cards = drawRandomCards(2, Collections.<Card>emptyList());
                leftRampCard = cards.get(0);
                rightRampCard = cards.get(1);
            }
        }
        rebuildShapes(field);
    }

    private void handleLeftRamp(Field field, Ball ball) {
        if (multiballStatus == MultiballStatus.INACTIVE) {
            collectCard(field, ball, leftRampCard);
        }
        else {
            scoreMultiballJackpot(field, ball);
        }
    }

    private void handleRightRamp(Field field, Ball ball) {
        if (multiballStatus == MultiballStatus.INACTIVE) {
            collectCard(field, ball, rightRampCard);
        }
        else {
            scoreMultiballJackpot(field, ball);
        }
    }

    private void handleCenterRamp(Field field, Ball ball) {
        if (multiballStatus == MultiballStatus.INACTIVE) {
            if (hand.isEmpty()) {
                hand.add(leftRampCard);
            }
            Card[] lucky = drawLuckyCards(hand);
            leftRampCard = lucky[0];
            rightRampCard = lucky[1];
            field.showGameMessage(field.resolveString("lucky_draw_message"), 2500);
            field.addScoreWithAnimation(CENTER_RAMP_SCORE, ball.getPosition());
            rebuildShapes(field);
        }
        else {
            scoreMultiballJackpot(field, ball);
        }
    }

    // ---- Delegate overrides ----

    @Override public void gameStarted(Field field) {
        launchBarrier = field.getFieldElementById("LaunchBarrier");
        launchBarrier.setRetracted(true);
        hand.clear();
        handBonus = 0;
        multiballStatus = MultiballStatus.INACTIVE;
        List<Card> cards = drawRandomCards(2, Collections.<Card>emptyList());
        leftRampCard = cards.get(0);
        rightRampCard = cards.get(1);
        rebuildShapes(field);
    }

    @Override public void ballLost(Field field) {
        launchBarrier.setRetracted(false);
    }

    @Override public void ballInSensorRange(Field field, SensorElement sensor, Ball ball) {
        String sensorId = sensor.getElementId();
        String prevSensorId = ball.getMostRecentSensorId();

        if ("LaunchBarrierSensor".equals(sensorId)) {
            launchBarrier.setRetracted(false);
        }
        else if ("LaunchBarrierRetract".equals(sensorId)) {
            launchBarrier.setRetracted(true);
        }
        else if ("LeftRampSensor_Trigger".equals(sensorId)
                && "LeftRampSensor_Enter".equals(prevSensorId)) {
            handleLeftRamp(field, ball);
        }
        else if ("RightRampSensor_Trigger".equals(sensorId)
                && "RightRampSensor_Enter".equals(prevSensorId)) {
            handleRightRamp(field, ball);
        }
        else if ("CenterRampSensor_Trigger".equals(sensorId)
                && "CenterRampSensor_Enter".equals(prevSensorId)) {
            handleCenterRamp(field, ball);
        }
        else if (sensorId != null && sensorId.startsWith("RampDropSensor")) {
            if (ball.getLayer() != 0 && ball.getLinearVelocity().len() < 0.05) {
                ball.moveToLayer(0);
            }
        }
    }

    @Override public void processCollision(Field field, FieldElement element, Body hitBody, Ball ball) {
        String id = element.getElementId();
        if (id.startsWith("RampEndCap")) {
            // The ball can bounce hard off the cap at the bottom of a ramp; bleed off most of its
            // velocity so it settles into the pocket instead of rebounding back down the ramp.
            // (The RampDropSensor there drops it to layer 0 once it's nearly stopped.)
            Vector2 v = ball.getLinearVelocity();
            ball.getBody().setLinearVelocity(v.x * 0.2f, v.y * 0.2f);
            return;
        }
        if ("LeftCycleTargets".equals(id)) {
            // If this is the last remaining target, allDropTargetsInGroupHit will draw a lucky card
            // and we don't want to do anything when this is subsequently called.
            if (leftRampCard != null && !((DropTargetGroupElement) element).allTargetsHit()) {
                List<Card> used = new ArrayList<>(hand);
                used.add(leftRampCard);
                used.add(rightRampCard);
                leftRampCard = drawRandomCard(used);
            }
            rebuildShapes(field);
        }
        else if ("RightCycleTargets".equals(id)) {
            if (rightRampCard != null && !((DropTargetGroupElement) element).allTargetsHit()) {
                List<Card> used = new ArrayList<>(hand);
                used.add(leftRampCard);
                used.add(rightRampCard);
                rightRampCard = drawRandomCard(used);
            }
            rebuildShapes(field);
        }
    }

    @Override public void allDropTargetsInGroupHit(
            Field field, DropTargetGroupElement targetGroup, Ball ball) {
        // Activate ball saver for left and right groups; draw lucky cards for the cycle banks.
        String id = targetGroup.getElementId();
        if ("DropTargetLeftSave".equals(id)) {
            WallElement saver = field.getFieldElementById("BallSaver-left");
            saver.setRetracted(false);
            field.showGameMessage(field.resolveString("left_save_enabled_message"), 1500);
        }
        else if ("DropTargetRightSave".equals(id)) {
            WallElement saver = field.getFieldElementById("BallSaver-right");
            saver.setRetracted(false);
            field.showGameMessage(field.resolveString("right_save_enabled_message"), 1500);
        }
        else if ("LeftCycleTargets".equals(id)) {
            if (hand.size() > 0 && hand.size() < 5) {
                leftRampCard = drawLuckyCards(hand)[0];
                rebuildShapes(field);
                field.showGameMessage(field.resolveString("lucky_card_message"), 2000);
            }
        }
        else if ("RightCycleTargets".equals(id)) {
            if (hand.size() > 0 && hand.size() < 5) {
                rightRampCard = drawLuckyCards(hand)[1];
                rebuildShapes(field);
                field.showGameMessage(field.resolveString("lucky_card_message"), 2000);
            }
        }
    }

    @Override public void allRolloversInGroupActivated(
            Field field, RolloverGroupElement rolloverGroup, Ball ball) {
        if ("FlipperRollovers".equals(rolloverGroup.getElementId())) {
            rolloverGroup.setAllRolloversActivated(false);
            field.incrementAndDisplayScoreMultiplier(1500);
        }
        else if ("TopRollovers".equals(rolloverGroup.getElementId())) {
            rolloverGroup.setAllRolloversActivated(false);
            handBonus += 1000;
            field.showGameMessage(field.resolveString("hand_bonus_increased_message"), 1500);
        }
    }

    @Override public void tick(Field field, long nanos) {
        if (multiballStatus == MultiballStatus.ACTIVE && field.getBalls().size() <= 1) {
            clearMultiballStatus(field);
        }
    }
}
