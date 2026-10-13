package com.khanyi.java.fundermentals.datastructures;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class FilteringIteratorTest {

    // ______________ filtering ______________

    @Test
    void returnsOnlyMatchingElementsInOrder() {
        Iterator<Integer> evens = new FilteringIterator<>(List.of(1, 2, 3, 4, 5, 6).iterator(), n -> n % 2 == 0);

        assertEquals(List.of(2, 4, 6), drain(evens));
    }

    @Test
    void noMatchesMeansNoElements() {
        Iterator<Integer> it = new FilteringIterator<>(List.of(1, 3, 5).iterator(), n -> n % 2 == 0);

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void emptySourceMeansNoElements() {
        Iterator<String> it = new FilteringIterator<>(List.<String>of().iterator(), s -> true);

        assertFalse(it.hasNext());
    }

    @Test
    void everythingMatchesGivesTheWholeSource() {
        Iterator<String> it = new FilteringIterator<>(List.of("a", "b", "c").iterator(), s -> true);

        assertEquals(List.of("a", "b", "c"), drain(it));
    }

    @Test
    void matchesAtTheVeryStartAndEndAreKept() {
        Iterator<String> it = new FilteringIterator<>(
                List.of("keep1", "x", "y", "keep2").iterator(), s -> s.startsWith("keep"));

        assertEquals(List.of("keep1", "keep2"), drain(it));
    }

    @Test
    void nextThrowsAfterTheLastMatch() {
        Iterator<Integer> it = new FilteringIterator<>(List.of(1, 2, 3).iterator(), n -> n == 2);

        assertEquals(2, it.next());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void nextWorksWithoutCallingHasNextFirst() {
        Iterator<Integer> it = new FilteringIterator<>(List.of(1, 2, 3, 4).iterator(), n -> n > 2);

        assertEquals(3, it.next());
        assertEquals(4, it.next());
    }

    // ______________ the Iterator contract ______________

    @Test
    void callingHasNextManyTimesDoesNotSkipElements() {
        Iterator<Integer> it = new FilteringIterator<>(List.of(1, 2, 3, 4).iterator(), n -> n % 2 == 0);

        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        assertTrue(it.hasNext());
        assertTrue(it.hasNext());
        assertEquals(4, it.next());
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }

    @Test
    void nullElementsThatPassTheFilterAreReturned() {
        // this is why the class uses a "ready" flag instead of checking nextMatch != null
        List<String> withNulls = Arrays.asList("a", null, "b", null);
        Iterator<String> it = new FilteringIterator<>(withNulls.iterator(), s -> s == null);

        assertTrue(it.hasNext());
        assertNull(it.next());
        assertTrue(it.hasNext());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void removeIsNotSupported() {
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3));
        Iterator<Integer> it = new FilteringIterator<>(source.iterator(), n -> true);
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove);
        assertEquals(List.of(1, 2, 3), source);
    }

    // ______________ laziness ______________

    @Test
    void nothingIsTestedUntilAsked() {
        AtomicInteger tests = new AtomicInteger();

        new FilteringIterator<>(List.of(1, 2, 3).iterator(), n -> { tests.incrementAndGet(); return true; });

        assertEquals(0, tests.get());
    }

    @Test
    void looksAheadOnlyAsFarAsTheNextMatch() {
        AtomicInteger tests = new AtomicInteger();
        Iterator<Integer> it = new FilteringIterator<>(List.of(1, 2, 3, 4, 5, 6).iterator(),
                n -> { tests.incrementAndGet(); return n % 2 == 0; });

        assertEquals(2, it.next());
        assertEquals(2, tests.get(), "only 1 and 2 should have been tested");
    }

    @Test
    void worksOnAnEndlessSource() {
        Iterator<Integer> naturals = Stream.iterate(1, n -> n + 1).iterator();
        Iterator<Integer> multiplesOf7 = new FilteringIterator<>(naturals, n -> n % 7 == 0);

        assertEquals(7, multiplesOf7.next());
        assertEquals(14, multiplesOf7.next());
        assertEquals(21, multiplesOf7.next());
    }

    // ______________ generics and stacking ______________

    @Test
    void acceptsAPredicateForAParentType() {
        // Predicate<Object> filtering Strings: allowed by Predicate<? super E>
        Predicate<Object> notNull = o -> o != null;
        Iterator<String> it = new FilteringIterator<>(Arrays.asList("a", null, "b").iterator(), notNull);

        assertEquals(List.of("a", "b"), drain(it));
    }

    @Test
    void canReadFromAnIteratorOfASubtype() {
        // Iterator<Integer> read as Iterator<Number>: allowed by Iterator<? extends E>
        Iterator<Number> it = new FilteringIterator<Number>(List.of(1, 2, 3).iterator(), n -> n.intValue() > 1);

        assertEquals(List.of(2, 3), drain(it));
    }

    @Test
    void filtersCanBeStacked() {
        Iterator<Integer> evens = new FilteringIterator<>(List.of(1, 2, 3, 4, 5, 6, 7, 8).iterator(), n -> n % 2 == 0);
        Iterator<Integer> evensOverFour = new FilteringIterator<>(evens, n -> n > 4);

        assertEquals(List.of(6, 8), drain(evensOverFour));
    }

    @Test
    void worksOverOurOwnLinkedPositionalList() {
        LinkedPositionalList<String> list = new LinkedPositionalList<>();
        list.addLast("rock");
        list.addLast("jazz");
        list.addLast("rock");
        list.addLast("pop");

        Iterator<String> rock = new FilteringIterator<>(list.iterator(), "rock"::equals);

        assertEquals(List.of("rock", "rock"), drain(rock));
        assertEquals(4, list.size());
    }

    // ______________ constructor ______________

    @Test
    void nullSourceOrFilterIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new FilteringIterator<String>(null, s -> true));
        assertThrows(IllegalArgumentException.class, () -> new FilteringIterator<>(List.of("a").iterator(), null));
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
