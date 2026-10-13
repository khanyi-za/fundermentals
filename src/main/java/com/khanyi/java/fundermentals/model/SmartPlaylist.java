package com.khanyi.java.fundermentals.model;

import com.khanyi.java.fundermentals.datastructures.FilteringIterator;

import java.util.Iterator;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * A playlist that fills itself from a rule, e.g. "hip-hop" or "120 to 130 BPM".
 *
 *   SmartPlaylist gym = SmartPlaylist.byTempo("p2", "Gym", 120, 130, 50);
 *   gym.refresh(repository.sorted(MOST_POPULAR_FIRST));   // the 50 most popular matches
 *
 * It is still a Playlist: you can reorder, remove, and add by hand,
 * but only tracks that match the rule are let in.
 */
public class SmartPlaylist extends Playlist {

    private final String ruleDescription;
    private final Predicate<? super Track> rule;
    private final int limit;

    public SmartPlaylist(String id, String name, String ruleDescription,
                         Predicate<? super Track> rule, int limit) {
        super(id, name);
        if (ruleDescription == null || ruleDescription.isBlank()) {
            throw new IllegalArgumentException("ruleDescription must not be blank");
        }
        if (rule == null) {
            throw new IllegalArgumentException("rule must not be null");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1, was " + limit);
        }
        this.ruleDescription = ruleDescription.strip();
        this.rule = rule;
        this.limit = limit;
    }

    // ______________ ready-made rules ______________

    public static SmartPlaylist byGenre(String id, String name, String genre, int limit) {
        if (genre == null || genre.isBlank()) {
            throw new IllegalArgumentException("genre must not be blank");
        }
        String wanted = genre.strip();
        return new SmartPlaylist(id, name, "genre is " + wanted.toLowerCase(Locale.ROOT),
                track -> track.getGenre().equalsIgnoreCase(wanted), limit);
    }

    public static SmartPlaylist byTempo(String id, String name, double minBpm, double maxBpm, int limit) {
        if (!(minBpm >= 0 && minBpm <= maxBpm)) {   // also false for NaN
            throw new IllegalArgumentException("need 0 <= minBpm <= maxBpm, got " + minBpm + " and " + maxBpm);
        }
        return new SmartPlaylist(id, name, "tempo " + minBpm + " to " + maxBpm + " BPM",
                track -> track.getTempo() >= minBpm && track.getTempo() <= maxBpm, limit);
    }

    // ______________ the rule ______________

    public String getRuleDescription() {
        return ruleDescription;
    }

    public int getLimit() {
        return limit;
    }

    public boolean matches(MediaItem item) {
        return item instanceof Track track && rule.test(track);
    }

    /**
     * Throws away the current contents and refills from the library, in the library's order,
     * stopping at the limit. Pass a sorted list to choose which matches win.
     *
     * @return how many tracks were added
     */
    public int refresh(Iterable<? extends Track> library) {
        if (library == null) {
            throw new IllegalArgumentException("library must not be null");
        }
        while (!isEmpty()) {
            removeAt(size() - 1);   // from the end: positionAt(last) needs no walking
        }
        Iterator<Track> matching = new FilteringIterator<>(library.iterator(), rule);
        while (size() < limit && matching.hasNext()) {
            add(matching.next());
        }
        return size();
    }

    // ______________ the hook from Playlist ______________

    /** Only tracks that match the rule get in, whether added by hand or by refresh. */
    @Override
    protected void checkCanAdd(MediaItem item) {
        if (!matches(item)) {
            throw new IllegalArgumentException(
                    "'" + item.getTitle() + "' doesn't match this playlist's rule: " + ruleDescription);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " where " + ruleDescription;
    }
}
