package com.khanyi.java.fundermentals.datastructures;

/**
 * A list whose elements are accessed by Position rather than by index.
 */
public interface PositionalList<E> extends Iterable<E> {

    int size();

    boolean isEmpty();

    /**
     * @return the first position, or null if the list is empty
     */
    Position<E> first();

    /**
     * @return the last position, or null if the list is empty
     */
    Position<E> last();

    /**
     * @return the position just before p, or null if p is first
     */
    Position<E> before(Position<E> p) throws IllegalArgumentException;

    /**
     * @return the position just after p, or null if p is last
     */
    Position<E> after(Position<E> p) throws IllegalArgumentException;

    Position<E> addFirst(E e);

    Position<E> addLast(E e);

    Position<E> addBefore(Position<E> p, E e) throws IllegalArgumentException;

    Position<E> addAfter(Position<E> p, E e) throws IllegalArgumentException;

    /**
     * Replaces the element at p and returns the old one.
     */
    E set(Position<E> p, E e) throws IllegalArgumentException;

    /**
     * Removes the element at p and returns it. p becomes invalid.
     */
    E remove(Position<E> p) throws IllegalArgumentException;

    /**
     * @return an iterable view of the positions, from first to last
     */
    Iterable<Position<E>> positions();
}
