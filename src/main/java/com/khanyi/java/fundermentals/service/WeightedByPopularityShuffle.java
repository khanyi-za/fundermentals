package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Track;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * A shuffle that favours popular tracks: they tend to come early, but nothing is guaranteed,
 * and even a popularity-0 track can come first now and then.
 *
 * The chance of an item coming first is its weight divided by the total weight.
 * Weight = popularity + 1, so every track has a chance; podcast episodes have no
 * popularity and get a middle weight.
 */
public final class WeightedByPopularityShuffle implements ShuffleStrategy {

    static final int EPISODE_WEIGHT = 51;   // the same as a track with popularity 50

    private final Random random;

    public WeightedByPopularityShuffle(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    /**
     * Efraimidis-Spirakis: give each item a random key u^(1/weight), where u is uniform in [0, 1),
     * then sort by key, biggest first. Heavier items tend to get keys closer to 1. O(n log n).
     */
    @Override
    public List<MediaItem> order(List<MediaItem> items) {
        List<Keyed> keyed = new ArrayList<>(items.size());
        for (MediaItem item : items) {
            double key = Math.pow(random.nextDouble(), 1.0 / weightOf(item));
            keyed.add(new Keyed(item, key));
        }
        keyed.sort(Comparator.comparingDouble(Keyed::key).reversed());

        List<MediaItem> result = new ArrayList<>(keyed.size());
        for (Keyed k : keyed) {
            result.add(k.item());
        }
        return result;
    }

    static int weightOf(MediaItem item) {
        return item instanceof Track track ? track.getPopularity() + 1 : EPISODE_WEIGHT;
    }

    /** An item paired with its random sort key. A record: just data, with equals/hashCode/toString for free. */
    private record Keyed(MediaItem item, double key) {
    }
}
