package com.khanyi.java.fundermentals.datastructures;

/**
 * A FIFO QUEUE BUILT ON THE DoublyLinkedList
 * Elements join at the back (addLast) and leave from the front (removeFirst), both O(1).
 */
public class LinkedQueue<E> implements Queue<E>{

    private final DoublyLinkedList<E> list = new DoublyLinkedList<>();

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public void enqueue(E e) {
        list.addLast(e);
    }

    @Override
    public E dequeue() throws EmptyCollectionException {
        requireNotEmpty();
        return list.removeFirst();
    }

    @Override
    public E first() throws EmptyCollectionException {
        requireNotEmpty();
        return list.first();
    }

    @Override
    public String toString() {
        return list.toString();   // front of the queue is printed first
    }

    private void requireNotEmpty() {
        if (isEmpty()) {
            throw new EmptyCollectionException("Queue is empty");
        }
    }
}
