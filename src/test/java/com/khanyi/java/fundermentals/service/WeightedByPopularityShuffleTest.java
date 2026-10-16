package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.PodcastEpisode;
import com.khanyi.java.fundermentals.model.Track;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class WeightedByPopularityShuffleTest {

    @Test
    void weightIsPopularityPlusOne() {
        assertEquals(1, WeightedByPopularityShuffle.weightOf(track("a", 0)));
        assertEquals(51, WeightedByPopularityShuffle.weightOf(track("a", 50)));
        assertEquals(101, WeightedByPopularityShuffle.weightOf(track("a", 100)));
    }

    @Test
    void podcastEpisodesGetAMiddleWeight() {
        PodcastEpisode episode = new PodcastEpisode("ep1", "Pilot", Duration.ofMinutes(30), "Show", 1);

        assertEquals(WeightedByPopularityShuffle.EPISODE_WEIGHT, WeightedByPopularityShuffle.weightOf(episode));
        assertEquals(51, WeightedByPopularityShuffle.EPISODE_WEIGHT);
    }

    @Test
    void chanceOfComingFirstIsWeightOverTotalWeight() {
        // popularity 0, 49, 99 -> weights 1, 50, 100, total 151
        WeightedByPopularityShuffle shuffle = new WeightedByPopularityShuffle(new Random(7));
        List<MediaItem> items = List.of(track("low", 0), track("mid", 49), track("high", 99));
        Map<String, Integer> firsts = new HashMap<>();
        int runs = 100_000;

        for (int i = 0; i < runs; i++) {
            firsts.merge(shuffle.order(items).get(0).getId(), 1, Integer::sum);
        }

        assertEquals(runs * 1 / 151.0, firsts.getOrDefault("low", 0), runs * 0.005);
        assertEquals(runs * 50 / 151.0, firsts.get("mid"), runs * 0.01);
        assertEquals(runs * 100 / 151.0, firsts.get("high"), runs * 0.01);
    }

    @Test
    void anUnpopularTrackCanStillComeFirst() {
        WeightedByPopularityShuffle shuffle = new WeightedByPopularityShuffle(new Random(11));
        List<MediaItem> items = List.of(track("hot", 100), track("cold", 0));
        boolean coldFirst = false;

        for (int i = 0; i < 2_000 && !coldFirst; i++) {
            coldFirst = shuffle.order(items).get(0).getId().equals("cold");
        }

        assertTrue(coldFirst, "weight + 1 means no track is ever impossible");
    }

    @Test
    void equalPopularityIsAFairShuffle() {
        WeightedByPopularityShuffle shuffle = new WeightedByPopularityShuffle(new Random(5));
        List<MediaItem> items = List.of(track("a", 60), track("b", 60));
        int aFirst = 0;
        int runs = 20_000;

        for (int i = 0; i < runs; i++) {
            if (shuffle.order(items).get(0).getId().equals("a")) {
                aFirst++;
            }
        }

        assertEquals(runs / 2.0, aFirst, runs * 0.02);
    }

    @Test
    void theSameSeedGivesTheSameOrder() {
        List<MediaItem> items = List.of(track("a", 0), track("b", 49), track("c", 99));

        assertEquals(new WeightedByPopularityShuffle(new Random(42)).order(items),
                new WeightedByPopularityShuffle(new Random(42)).order(items));
    }

    @Test
    void nullRandomIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new WeightedByPopularityShuffle(null));
    }

    private static Track track(String id, int popularity) {
        return new Track.Builder()
                .id(id)
                .title(id)
                .artists("Artist")
                .album("Album")
                .genre("pop")
                .popularity(popularity)
                .duration(Duration.ofSeconds(60))
                .build();
    }
}
