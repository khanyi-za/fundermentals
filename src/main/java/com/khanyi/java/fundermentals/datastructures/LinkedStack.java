package com.khanyi.java.fundermentals.datastructures;

/**
 * A LIFO stack built ontop of the DoublyLinkedList
 * The top of the stack is the front of the list, the methods pop and push are O(1).
 */
public class LinkedStack<E> implements Stack<E> {

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
    public void push(E e) {
        list.addFirst(e);
    }

    @Override
    public E pop() throws EmptyCollectionException {
        requireNotEmpty();
        return list.removeFirst();
    }

    @Override
    public E peek() throws EmptyCollectionException {
        requireNotEmpty();
        return list.first();
    }

    @Override
    public String toString() {
        return list.toString(); // the top of the stack is printed first
    }

    private void requireNotEmpty() {
        if (isEmpty()) {
            throw new EmptyCollectionException("Stack is empty");
        }
    }
}
