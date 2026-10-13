package com.khanyi.java.fundermentals.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Goes through an Iterable, then starts again from the beginning: "repeat all" for a playlist.
 *
 *   Iterator<Track> loop = RepeatingIterator.forever(playlist);      // never ends (unless empty)
 *   Iterator<Track> twice = RepeatingIterator.times(playlist, 2);    // two full passes, then stops
 *
 * It needs an Iterable, not an Iterator: an Iterator can only be used once,
 * but an Iterable can hand out a fresh iterator for every pass.
 */
public class RepeatingIterator<E> implements Iterator<E> {

    private static final int FOREVER = -1;

    private final Iterable<? extends E> source;
    private final int maxPasses;           // FOREVER, or 1 and up

    private Iterator<? extends E> current;
    private int pass = 1;                  // which pass we are on, starting at 1

    /** Private: use forever(...) or times(...), whose names say what you get. */
    private RepeatingIterator(Iterable<? extends E> source, int maxPasses) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.source = source;
        this.maxPasses = maxPasses;
        this.current = source.iterator();
    }

    /** Loops until the caller stops asking. Stops at once if the source is empty. */
    public static <E> RepeatingIterator<E> forever(Iterable<? extends E> source) {
        return new RepeatingIterator<>(source, FOREVER);
    }

    /** Goes through the source exactly the given number of times. */
    public static <E> RepeatingIterator<E> times(Iterable<? extends E> source, int passes) {
        if (passes < 1) {
            throw new IllegalArgumentException("passes must be at least 1, was " + passes);
        }
        return new RepeatingIterator<>(source, passes);
    }

    @Override
    public boolean hasNext() {
        if (current.hasNext()) {
            return true;
        }
        if (maxPasses != FOREVER && pass >= maxPasses) {
            return false;                  // used up every pass we were allowed
        }
        Iterator<? extends E> fresh = source.iterator();
        if (!fresh.hasNext()) {
            return false;                  // empty source: stop instead of spinning forever
        }
        current = fresh;
        pass++;
        return true;
    }

    @Override
    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements");
        }
        return current.next();
    }

    /** 1 on the first time through, 2 on the second, and so on. */
    public int currentPass() {
        return pass;
    }

    // remove() is not supported (Iterator's default throws UnsupportedOperationException).
    // A "repeat" view shouldn't delete from the playlist, and after hasNext() starts a new pass,
    // the current iterator is no longer the one that returned the last element.
}
