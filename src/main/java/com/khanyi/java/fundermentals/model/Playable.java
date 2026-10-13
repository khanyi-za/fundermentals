package com.khanyi.java.fundermentals.model;

import java.time.Duration;

/**
 * something that can be played has a running time.
 * Tracks and podcasts episodes are both Playable, so a playlist of queue
 * holds either track or episode without knowing which one it has
 */
public interface Playable {

    /**
     * @return how long this item plays for
     */
    Duration getDuration();

    /**
     * @return the name shown  in a player, e.g. "Artist - Title"
     */
    String getDisplayName();

    /**
     * formats the duration as m:ss, or h:m:ss.
     * A default method for every playable item, can override it
     */
    default String formattedDuration() {
        Duration d = getDuration();
        long hours = d.toHours();
        int minutes = d.toMinutesPart();
        int seconds = d.toSecondsPart();
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

}
