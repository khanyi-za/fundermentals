package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.PodcastEpisode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static com.khanyi.java.fundermentals.service.EditTestSupport.track;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The rules every ShuffleStrategy must follow (from the interface's Javadoc),
 * checked once for every implementation.
 */
public class ShuffleStrategyContractTest {

    static Stream<ShuffleStrategy> strategies() {
        return Stream.of(
                new InOrderStrategy(),
                new RandomShuffle(new Random(42)),
                new WeightedByPopularityShuffle(new Random(42)));
    }

    private static List<MediaItem> sample() {
        List<MediaItem> items = new ArrayList<>();
        for (String id : List.of("a", "b", "c", "d", "e", "f", "g", "h")) {
            items.add(track(id));
        }
        items.add(items.get(0));   // a duplicate
        items.add(new PodcastEpisode("ep1", "Pilot", Duration.ofMinutes(30), "Show", 1));
        return items;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    void returnsTheSameItemsTheSameNumberOfTimes(ShuffleStrategy strategy) {
        List<MediaItem> input = sample();

        List<MediaItem> result = strategy.order(input);

        assertEquals(sortedIds(input), sortedIds(result));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    void neverChangesTheInput(ShuffleStrategy strategy) {
        List<MediaItem> input = sample();
        List<MediaItem> before = List.copyOf(input);

        strategy.order(input);

        assertEquals(before, input);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    void returnsANewList(ShuffleStrategy strategy) {
        List<MediaItem> input = sample();

        List<MediaItem> result = strategy.order(input);

        assertNotSame(input, result);
        result.clear();   // the caller may do what it likes with it
        assertEquals(10, input.size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    void returnsANewListEvenForAnImmutableInput(ShuffleStrategy strategy) {
        List<MediaItem> immutable = List.copyOf(sample());

        assertNotSame(immutable, strategy.order(immutable));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    void handlesEmptyAndSingleItemLists(ShuffleStrategy strategy) {
        assertTrue(strategy.order(List.of()).isEmpty());

        MediaItem only = track("x");
        assertEquals(List.of(only), strategy.order(List.of(only)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("strategies")
    void nameIsTheSimpleClassName(ShuffleStrategy strategy) {
        assertEquals(strategy.getClass().getSimpleName(), strategy.name());
    }

    @Test
    void anyLambdaCanBeAStrategy() {
        // @FunctionalInterface: one abstract method, so a lambda fits
        ShuffleStrategy reversed = items -> {
            List<MediaItem> copy = new ArrayList<>(items);
            java.util.Collections.reverse(copy);
            return copy;
        };

        assertEquals(List.of("c", "b", "a"),
                reversed.order(List.of(track("a"), track("b"), track("c"))).stream().map(MediaItem::getId).toList());
    }

    @Test
    void inOrderKeepsTheOrder() {
        List<MediaItem> input = sample();

        assertEquals(input, new InOrderStrategy().order(input));
    }

    private static List<String> sortedIds(List<MediaItem> items) {
        return items.stream().map(MediaItem::getId).sorted(Comparator.naturalOrder()).toList();
    }
}
