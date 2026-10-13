package com.khanyi.java.fundermentals.datastructures;

/**
 * Last-in, first-out (LIFO) collection.
 */
public interface Stack<E> {
    int size();

    boolean isEmpty();

    void push(E e);

    /**
     * removes and returns the top element.
     */
    E pop() throws EmptyCollectionException;

    /**
     * returns the top element, does not remove it
     */
    E peek() throws EmptyCollectionException;
}
