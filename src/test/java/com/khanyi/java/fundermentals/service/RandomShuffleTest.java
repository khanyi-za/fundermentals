package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.khanyi.java.fundermentals.service.EditTestSupport.track;
import static org.junit.jupiter.api.Assertions.*;

public class RandomShuffleTest {

    private final List<MediaItem> abc = List.of(track("a"), track("b"), track("c"));

    @Test
    void theSameSeedGivesTheSameOrder() {
        List<MediaItem> first = new RandomShuffle(new Random(42)).order(abc);
        List<MediaItem> second = new RandomShuffle(new Random(42)).order(abc);

        assertEquals(first, second);
    }

    @Test
    void aFixedSeedGivesAKnownOrder() {
        // with an injected Random, a test can assert the exact "random" result
        assertEquals("bac", ids(new RandomShuffle(new Random(42)).order(abc)));
    }

    @Test
    void differentCallsGiveDifferentOrders() {
        RandomShuffle shuffle = new RandomShuffle(new Random(7));
        List<MediaItem> many = List.of(track("a"), track("b"), track("c"), track("d"), track("e"), track("f"));

        assertNotEquals(shuffle.order(many), shuffle.order(many), "the Random moves on between calls");
    }

    @Test
    void everyOrderIsEquallyLikely() {
        // Fisher-Yates is fair: each of the 3! = 6 orders should come up about 1/6 of the time
        RandomShuffle shuffle = new RandomShuffle(new Random(1));
        Map<String, Integer> counts = new HashMap<>();
        int runs = 60_000;

        for (int i = 0; i < runs; i++) {
            counts.merge(ids(shuffle.order(abc)), 1, Integer::sum);
        }

        assertEquals(6, counts.size(), "every order must be possible, including leaving items in place");
        for (int count : counts.values()) {
            assertEquals(runs / 6.0, count, runs * 0.01, "each order about 10,000 times");
        }
    }

    @Test
    void itemsCanStayWhereTheyAre() {
        // the nextInt(i + 1) detail: with nextInt(i) "abc" could never come out
        RandomShuffle shuffle = new RandomShuffle(new Random(3));
        boolean sawOriginalOrder = false;
        for (int i = 0; i < 100 && !sawOriginalOrder; i++) {
            sawOriginalOrder = ids(shuffle.order(abc)).equals("abc");
        }

        assertTrue(sawOriginalOrder);
    }

    @Test
    void nullRandomIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RandomShuffle(null));
    }

    private static String ids(List<MediaItem> items) {
        return String.join("", items.stream().map(MediaItem::getId).toList());
    }
}
