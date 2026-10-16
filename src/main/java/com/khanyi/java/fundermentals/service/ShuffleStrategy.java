package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;

import java.util.List;

/**
 * Decides the order a playlist's items are played in: the Strategy pattern.
 *
 * The player holds one ShuffleStrategy and can be given a different one at any time
 * (setter injection), so "in order", "random" and "weighted" are interchangeable
 * without the player knowing which one it has.
 *
 * Contract for every implementation:
 *   - return a NEW list; never change the list passed in
 *   - the result holds exactly the same items, the same number of times, just reordered
 */
@FunctionalInterface
public interface ShuffleStrategy {

    /** @return the items in the order they should be played */
    List<MediaItem> order(List<MediaItem> items);

    /** A short label for logs and the API, e.g. "RandomShuffle". Implementations may override it. */
    default String name() {
        return getClass().getSimpleName();
    }
}
