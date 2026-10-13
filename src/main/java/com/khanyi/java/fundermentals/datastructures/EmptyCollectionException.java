package com.khanyi.java.fundermentals.datastructures;

/**
 * Thrown when an operation needs an element but the collection is empty,
 * e.g. popping an empty stack or dequeuing an empty queue.
 */
public class EmptyCollectionException extends RuntimeException {
    public EmptyCollectionException(String message) {
        super(message);
    }
}
