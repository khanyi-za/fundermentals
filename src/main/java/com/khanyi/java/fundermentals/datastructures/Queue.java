package com.khanyi.java.fundermentals.datastructures;

/**
 * First-in, first-out (FIFO) collection.
 */

public interface Queue<E> {

    int size();

    boolean isEmpty();

    /**
     * adding an item to the queue
     */
    void enqueue(E e);

    /**
     * removes and returns the element at the front.
     */
    E dequeue() throws EmptyCollectionException;

    /**
     * returns without removing the 1st element
     */
    E first() throws EmptyCollectionException;
}
