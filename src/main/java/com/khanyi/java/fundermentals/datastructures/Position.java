package com.khanyi.java.fundermentals.datastructures;

/**
 * A handle to a single location in a positional list.
 * The position stays valid even if other elements are added or removed around it.
 */
public interface Position<E> {

    /**
     * @return the element stored at this position
     * @throws IllegalStateException if this position is no longer valid
     */
    E getElement() throws IllegalStateException;
}
