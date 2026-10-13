package com.khanyi.java.fundermentals.datastructures;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class RepeatingIteratorTest {

    private final List<String> abc = List.of("a", "b", "c");

    // ______________ times ______________

    @Test
    void timesOneIsASinglePass() {
        assertEquals(List.of("a", "b", "c"), drain(RepeatingIterator.times(abc, 1)));
    }

    @Test
    void timesTwoGoesThroughTwice() {
        assertEquals(List.of("a", "b", "c", "a", "b", "c"), drain(RepeatingIterator.times(abc, 2)));
    }

    @Test
    void nextThrowsWhenThePassesAreUsedUp() {
        RepeatingIterator<String> it = RepeatingIterator.times(abc, 1);
        drain(it);

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void timesRejectsLessThanOnePass() {
        assertThrows(IllegalArgumentException.class, () -> RepeatingIterator.times(abc, 0));
        assertThrows(IllegalArgumentException.class, () -> RepeatingIterator.times(abc, -3));
    }

    // ______________ forever ______________

    @Test
    void foreverKeepsGoingRoundTheSource() {
        RepeatingIterator<String> it = RepeatingIterator.forever(abc);
        StringBuilder played = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            played.append(it.next());
        }

        assertEquals("abcabcabca", played.toString());
        assertTrue(it.hasNext());
    }

    @Test
    void foreverOverOneElementRepeatsIt() {
        RepeatingIterator<String> it = RepeatingIterator.forever(List.of("x"));

        assertEquals("x", it.next());
        assertEquals("x", it.next());
        assertEquals("x", it.next());
        assertEquals(3, it.currentPass());
    }

    // ______________ empty sources ______________

    @Test
    void foreverOverAnEmptySourceStopsAtOnce() {
        RepeatingIterator<String> it = RepeatingIterator.forever(List.of());

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void timesOverAnEmptySourceStopsAtOnce() {
        assertTrue(drain(RepeatingIterator.times(List.of(), 5)).isEmpty());
    }

    @Test
    void sourceEmptiedBetweenPassesStopsTheLoop() {
        // LinkedPositionalList, not ArrayList: ArrayList's iterator checks cursor != size,
        // so emptying it after the last next() would make the old iterator claim there is more
        LinkedPositionalList<String> source = new LinkedPositionalList<>();
        source.addLast("a");
        source.addLast("b");
        RepeatingIterator<String> it = RepeatingIterator.forever(source);
        it.next();
        it.next();

        while (!source.isEmpty()) {   // the first pass is finished, so this is between passes
            source.remove(source.first());
        }

        assertFalse(it.hasNext());
    }

    // ______________ passes ______________

    @Test
    void currentPassCountsFromOne() {
        RepeatingIterator<String> it = RepeatingIterator.times(abc, 2);
        StringBuilder trace = new StringBuilder();

        while (it.hasNext()) {
            trace.append(it.next()).append(it.currentPass());
        }

        assertEquals("a1b1c1a2b2c2", trace.toString());
    }

    @Test
    void passOnlyMovesOnWhenTheNextPassStarts() {
        RepeatingIterator<String> it = RepeatingIterator.forever(abc);
        it.next();
        it.next();
        it.next();

        assertEquals(1, it.currentPass(), "still on pass 1 until hasNext/next starts pass 2");
        it.hasNext();
        assertEquals(2, it.currentPass());
    }

    @Test
    void lastPassDoesNotGoPastTheLimit() {
        RepeatingIterator<String> it = RepeatingIterator.times(abc, 2);
        drain(it);
        it.hasNext();

        assertEquals(2, it.currentPass());
    }

    // ______________ the Iterator contract ______________

    @Test
    void callingHasNextManyTimesDoesNotSkipOrRestart() {
        RepeatingIterator<String> it = RepeatingIterator.times(List.of("a", "b"), 2);

        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals(2, it.currentPass(), "repeated hasNext must not start extra passes");
        assertEquals("a", it.next());
    }

    @Test
    void removeIsNotSupported() {
        List<String> source = new ArrayList<>(abc);
        RepeatingIterator<String> it = RepeatingIterator.forever(source);
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove);
        assertEquals(abc, source);
    }

    @Test
    void nullSourceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RepeatingIterator.forever(null));
        assertThrows(IllegalArgumentException.class, () -> RepeatingIterator.times(null, 2));
    }

    // ______________ generics and stacking ______________

    @Test
    void canReadAnIterableOfASubtype() {
        // List<Integer> repeated as RepeatingIterator<Number>: allowed by Iterable<? extends E>
        RepeatingIterator<Number> it = RepeatingIterator.times(List.of(1, 2), 2);

        assertEquals(List.<Number>of(1, 2, 1, 2), drain(it));
    }

    @Test
    void worksOverOurOwnLinkedPositionalList() {
        LinkedPositionalList<String> list = new LinkedPositionalList<>();
        list.addLast("intro");
        list.addLast("chorus");

        assertEquals(List.of("intro", "chorus", "intro", "chorus"), drain(RepeatingIterator.times(list, 2)));
    }

    @Test
    void stacksWithFilteringIterator() {
        // an endless loop, filtered down: only the songs starting with "r", round and round
        List<String> playlist = List.of("rock1", "jazz", "rock2", "pop");
        Iterator<String> rockLoop = new FilteringIterator<>(RepeatingIterator.forever(playlist), s -> s.startsWith("r"));

        assertEquals("rock1", rockLoop.next());
        assertEquals("rock2", rockLoop.next());
        assertEquals("rock1", rockLoop.next());
        assertEquals("rock2", rockLoop.next());
    }

    @Test
    void filteringAFiniteRepeatStopsCleanly() {
        Iterator<String> it = new FilteringIterator<>(RepeatingIterator.times(abc, 3), "b"::equals);

        assertEquals(List.of("b", "b", "b"), drain(it));
    }

    // ______________ helpers ______________

    private static <T> List<T> drain(Iterator<T> it) {
        List<T> out = new ArrayList<>();
        while (it.hasNext()) {
            out.add(it.next());
        }
        return out;
    }
}
