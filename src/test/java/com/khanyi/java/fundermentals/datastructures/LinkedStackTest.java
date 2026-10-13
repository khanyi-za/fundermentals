package com.khanyi.java.fundermentals.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LinkedStackTest {

    // Declared as the interface type: the tests only rely on the Stack contract.
    private Stack<String> stack;

    @BeforeEach
    void setUp() {
        stack = new LinkedStack<>();
    }

    @Test
    void newStackIsEmpty() {
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    void popOnEmptyStackThrows() {
        EmptyCollectionException ex = assertThrows(EmptyCollectionException.class, () -> stack.pop());
        assertEquals("Stack is empty", ex.getMessage());
    }

    @Test
    void peekOnEmptyStackThrows() {
        assertThrows(EmptyCollectionException.class, () -> stack.peek());
    }

    @Test
    void pushIncreasesSize() {
        stack.push("A");
        stack.push("B");

        assertFalse(stack.isEmpty());
        assertEquals(2, stack.size());
    }

    @Test
    void popReturnsElementsInLastInFirstOutOrder() {
        stack.push("A");
        stack.push("B");
        stack.push("C");

        assertEquals("C", stack.pop());
        assertEquals("B", stack.pop());
        assertEquals("A", stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    void peekReturnsTopWithoutRemovingIt() {
        stack.push("A");
        stack.push("B");

        assertEquals("B", stack.peek());
        assertEquals("B", stack.peek());
        assertEquals(2, stack.size());
    }

    @Test
    void popAfterEmptyingThrows() {
        stack.push("A");
        stack.pop();

        assertThrows(EmptyCollectionException.class, () -> stack.pop());
    }

    @Test
    void interleavedPushAndPop() {
        stack.push("A");
        stack.push("B");
        assertEquals("B", stack.pop());

        stack.push("C");
        assertEquals("C", stack.pop());
        assertEquals("A", stack.pop());
    }

    @Test
    void toStringPrintsTopFirst() {
        stack.push("A");
        stack.push("B");
        stack.push("C");

        assertEquals("[C, B, A]", stack.toString());
    }
}
