package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A fair shuffle: every possible order is equally likely.
 *
 * The Random is injected, so tests can pass new Random(42) and get the same
 * "random" order every run, while the real app passes new Random().
 */
public final class RandomShuffle implements ShuffleStrategy {

    private final Random random;

    public RandomShuffle(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = random;
    }

    /** Fisher-Yates: walk from the back, swapping each slot with a random slot at or before it. O(n). */
    @Override
    public List<MediaItem> order(List<MediaItem> items) {
        List<MediaItem> result = new ArrayList<>(items);
        for (int i = result.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);   // 0..i inclusive: an item may stay where it is
            swap(result, i, j);
        }
        return result;
    }

    private static void swap(List<MediaItem> list, int i, int j) {
        MediaItem temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
