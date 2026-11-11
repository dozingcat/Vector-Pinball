package com.dozingcatsoftware.vectorpinball.fields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.dozingcatsoftware.vectorpinball.fields.Field9Delegate.Card;
import com.dozingcatsoftware.vectorpinball.fields.Field9Delegate.HandResult;

/**
 * Local JUnit tests for the pure poker logic in {@link Field9Delegate} (the "Royal Flush" table).
 * These exercise only the rank/straight/lucky-draw/hand-evaluation helpers, which don't touch the
 * Field, Box2D, or any Android classes, so they run on the JVM without a device.
 */
public class Field9DelegateTest {

    private static Card c(int rank, String suit) {
        return new Card(rank, suit);
    }

    /** The lower-ranked of the two lucky cards. */
    private static Card lower(Card[] cards) {
        return cards[0].rank <= cards[1].rank ? cards[0] : cards[1];
    }

    /** The higher-ranked of the two lucky cards. */
    private static Card higher(Card[] cards) {
        return cards[0].rank > cards[1].rank ? cards[0] : cards[1];
    }

    private static List<Integer> sortedRanks(Card[] cards) {
        List<Integer> ranks = new ArrayList<>();
        for (Card card : cards) ranks.add(card.rank);
        Collections.sort(ranks);
        return ranks;
    }

    private static HandResult eval(Card... cards) {
        Field9Delegate delegate = new Field9Delegate();
        delegate.hand.addAll(Arrays.asList(cards));
        return delegate.evaluateHand();
    }

    @Test public void bestRankForStraight_singleCard() {
        assertEquals(Integer.valueOf(10), Field9Delegate.bestRankForStraight(Arrays.asList(9)));
        assertEquals(Integer.valueOf(11), Field9Delegate.bestRankForStraight(Arrays.asList(10)));
        assertEquals(Integer.valueOf(10), Field9Delegate.bestRankForStraight(Arrays.asList(11)));
        assertEquals(Integer.valueOf(13), Field9Delegate.bestRankForStraight(Arrays.asList(14)));
    }

    @Test public void bestRankForStraight_multipleCards() {
        assertNull(Field9Delegate.bestRankForStraight(Arrays.asList(10, 5)));
        assertEquals(Integer.valueOf(9), Field9Delegate.bestRankForStraight(Arrays.asList(7, 8)));
        assertEquals(Integer.valueOf(12), Field9Delegate.bestRankForStraight(Arrays.asList(10, 13)));
        assertEquals(Integer.valueOf(9), Field9Delegate.bestRankForStraight(Arrays.asList(10, 6, 7)));
        assertNull(Field9Delegate.bestRankForStraight(Arrays.asList(10, 5, 7)));
        assertEquals(Integer.valueOf(6), Field9Delegate.bestRankForStraight(Arrays.asList(2, 4, 3, 5)));
        assertEquals(Integer.valueOf(7), Field9Delegate.bestRankForStraight(Arrays.asList(9, 5, 8, 6)));
        assertEquals(Integer.valueOf(10), Field9Delegate.bestRankForStraight(Arrays.asList(11, 14, 13, 12)));
    }

    @Test public void bestRankForStraight_wheel() {
        assertEquals(Integer.valueOf(5), Field9Delegate.bestRankForStraight(Arrays.asList(3, 14)));
        assertEquals(Integer.valueOf(3), Field9Delegate.bestRankForStraight(Arrays.asList(2, 14, 5, 4)));
    }

    @Test public void luckyDraw_pairAndStraight() {
        // Th 6h -> a 9h (toward a straight) and a pair of tens off-suit.
        Card[] lucky = new Field9Delegate().drawLuckyCards(Arrays.asList(c(10, "H"), c(6, "H")));
        assertEquals(c(9, "H"), lower(lucky));
        assertEquals(10, higher(lucky).rank);
        assertFalse("pair card should not duplicate the hand suit", higher(lucky).suit.equals("H"));
    }

    @Test public void luckyDraw_pairsWhenNoStraight() {
        // Th 5h -> no straight possible, so two off-suit pairs.
        Card[] lucky = new Field9Delegate().drawLuckyCards(Arrays.asList(c(10, "S"), c(5, "H")));
        assertEquals(Arrays.asList(5, 10), sortedRanks(lucky));
        if (lucky[0].rank == 5) {
            assertNotEquals("S", lucky[0].suit);
            assertNotEquals("H", lucky[1].suit);
        }
        else {
            assertNotEquals("H", lucky[0].suit);
            assertNotEquals("S", lucky[1].suit);
        }
    }

    @Test public void luckyDraw_straightFlush() {
        // Th 9h 8h 7h -> 6h / Jh to complete a straight flush.
        Card[] lucky = new Field9Delegate().drawLuckyCards(
                Arrays.asList(c(10, "H"), c(9, "H"), c(8, "H"), c(7, "H")));
        assertEquals(c(6, "H"), lower(lucky));
        assertEquals(c(11, "H"), higher(lucky));
    }

    @Test public void luckyDraw_flush() {
        // Ks 9s 8s 4s -> Qs / As to complete a flush.
        Card[] lucky = new Field9Delegate().drawLuckyCards(
                Arrays.asList(c(13, "S"), c(9, "S"), c(8, "S"), c(4, "S")));
        assertEquals(c(12, "S"), lower(lucky));
        assertEquals(c(14, "S"), higher(lucky));
    }

    @Test public void evaluateHand_ranks() {
        assertEquals("Royal Flush",
                eval(c(10, "H"), c(11, "H"), c(12, "H"), c(13, "H"), c(14, "H")).name);
        assertEquals("Straight Flush",
                eval(c(6, "S"), c(7, "S"), c(8, "S"), c(9, "S"), c(10, "S")).name);
        assertEquals("Four of a Kind",
                eval(c(9, "S"), c(9, "H"), c(9, "D"), c(9, "C"), c(2, "S")).name);
        assertEquals("Full House",
                eval(c(9, "S"), c(9, "H"), c(9, "D"), c(2, "C"), c(2, "S")).name);
        assertEquals("Flush",
                eval(c(2, "H"), c(5, "H"), c(8, "H"), c(11, "H"), c(13, "H")).name);
        assertEquals("Straight",
                eval(c(6, "S"), c(7, "H"), c(8, "S"), c(9, "S"), c(10, "S")).name);
        assertEquals("Three of a Kind",
                eval(c(9, "S"), c(9, "H"), c(9, "D"), c(4, "C"), c(2, "S")).name);
        assertEquals("Two Pair",
                eval(c(9, "S"), c(9, "H"), c(4, "D"), c(4, "C"), c(2, "S")).name);
        assertEquals("Pair",
                eval(c(9, "S"), c(9, "H"), c(7, "D"), c(4, "C"), c(2, "S")).name);
        assertEquals("High Card",
                eval(c(9, "S"), c(11, "H"), c(7, "D"), c(4, "C"), c(2, "S")).name);
    }

    @Test public void evaluateHand_wheelIsStraightNotRoyal() {
        // A-2-3-4-5 is the lowest straight; it must not be mistaken for a broadway/royal straight.
        HandResult wheel = eval(c(14, "S"), c(2, "H"), c(3, "S"), c(4, "S"), c(5, "S"));
        assertEquals("Straight", wheel.name);

        HandResult wheelFlush = eval(c(14, "S"), c(2, "S"), c(3, "S"), c(4, "S"), c(5, "S"));
        assertEquals("Straight Flush", wheelFlush.name);
    }
}
