package com.khanyi.java.fundermentals.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Wraps another iterator and only lets through the elements that pass a test, e.g.
 *
 *   Iterator<Track> fast = new FilteringIterator<>(tracks.iterator(), t -> t.getTempo() > 120);
 *
 * Lazy: nothing is copied and nothing is tested until hasNext() or next() is called.
 * It looks ahead exactly one element, so it works on a huge or even endless source.
 */
public class FilteringIterator<E> implements Iterator<E> {

    private final Iterator<? extends E> source;
    private final Predicate<? super E> filter;

    private E nextMatch;
    private boolean ready = false;   // true when nextMatch holds a match not yet returned

    public FilteringIterator(Iterator<? extends E> source, Predicate<? super E> filter) {
        if (source == null || filter == null) {
            throw new IllegalArgumentException("source and filter must not be null");
        }
        this.source = source;
        this.filter = filter;
    }

    /** Pulls from the source until it finds a match or runs out. Safe to call many times. */
    @Override
    public boolean hasNext() {
        while (!ready && source.hasNext()) {
            E candidate = source.next();
            if (filter.test(candidate)) {
                nextMatch = candidate;
                ready = true;
            }
        }
        return ready;
    }

    @Override
    public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more matching elements");
        }
        E result = nextMatch;
        nextMatch = null;   // don't keep a reference we no longer need
        ready = false;
        return result;
    }

    // remove() is not overridden, so it throws UnsupportedOperationException (Iterator's default).
    // By the time hasNext() has looked ahead, the source has already moved past the element
    // that next() returned, so asking the source to remove it would delete the wrong one.
}
