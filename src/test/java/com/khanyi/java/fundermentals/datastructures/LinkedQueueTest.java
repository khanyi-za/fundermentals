package com.khanyi.java.fundermentals.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LinkedQueueTest {

    // Declared as the interface type: the tests only rely on the Queue contract.
    private Queue<String> queue;

    @BeforeEach
    void setUp() {
        queue = new LinkedQueue<>();
    }

    @Test
    void newQueueIsEmpty() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void dequeueOnEmptyQueueThrows() {
        EmptyCollectionException ex = assertThrows(EmptyCollectionException.class, () -> queue.dequeue());
        assertEquals("Queue is empty", ex.getMessage());
    }

    @Test
    void firstOnEmptyQueueThrows() {
        assertThrows(EmptyCollectionException.class, () -> queue.first());
    }

    @Test
    void enqueueIncreasesSize() {
        queue.enqueue("A");
        queue.enqueue("B");

        assertFalse(queue.isEmpty());
        assertEquals(2, queue.size());
    }

    @Test
    void dequeueReturnsElementsInFirstInFirstOutOrder() {
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");

        assertEquals("A", queue.dequeue());
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void firstReturnsFrontWithoutRemovingIt() {
        queue.enqueue("A");
        queue.enqueue("B");

        assertEquals("A", queue.first());
        assertEquals("A", queue.first());
        assertEquals(2, queue.size());
    }

    @Test
    void dequeueAfterEmptyingThrows() {
        queue.enqueue("A");
        queue.dequeue();

        assertThrows(EmptyCollectionException.class, () -> queue.dequeue());
    }

    @Test
    void interleavedEnqueueAndDequeue() {
        queue.enqueue("A");
        queue.enqueue("B");
        assertEquals("A", queue.dequeue());

        queue.enqueue("C");
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.dequeue());
    }

    @Test
    void toStringPrintsFrontFirst() {
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");

        assertEquals("[A, B, C]", queue.toString());
    }
}
