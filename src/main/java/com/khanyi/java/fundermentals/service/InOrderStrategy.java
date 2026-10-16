package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;

import java.util.ArrayList;
import java.util.List;

/** No shuffle: plays the items in playlist order. The player's default strategy. */
public final class InOrderStrategy implements ShuffleStrategy {

    @Override
    public List<MediaItem> order(List<MediaItem> items) {
        return new ArrayList<>(items);
    }
}
