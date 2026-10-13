package com.khanyi.java.fundermentals.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class DoublyLinkedListTest {

    private DoublyLinkedList<String> list;

    @BeforeEach
    void setUp() {
        list = new DoublyLinkedList<>();
    }

    // ______________ empty list ______________

    @Test
    void newListIsEmpty() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    void firstAndLastThrowWhenEmpty() {
        assertThrows(EmptyCollectionException.class, () -> list.first());
        assertThrows(EmptyCollectionException.class, () -> list.last());
    }

    @Test
    void removeFirstAndRemoveLastThrowWhenEmpty() {
        assertThrows(EmptyCollectionException.class, () -> list.removeFirst());
        assertThrows(EmptyCollectionException.class, () -> list.removeLast());
    }

    // ______________ adding ______________

    @Test
    void addFirstOnEmptyListMakesItBothFirstAndLast() {
        list.addFirst("A");

        assertEquals("A", list.first());
        assertEquals("A", list.last());
        assertEquals(1, list.size());
    }

    @Test
    void addLastOnEmptyListMakesItBothFirstAndLast() {
        list.addLast("A");

        assertEquals("A", list.first());
        assertEquals("A", list.last());
        assertEquals(1, list.size());
    }

    @Test
    void addFirstPutsElementsAtTheFront() {
        list.addFirst("C");
        list.addFirst("B");
        list.addFirst("A");

        assertEquals("[A, B, C]", list.toString());
    }

    @Test
    void addLastPutsElementsAtTheBack() {
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");

        assertEquals("[A, B, C]", list.toString());
    }

    @Test
    void mixingAddFirstAndAddLastKeepsOrder() {
        list.addLast("B");
        list.addFirst("A");
        list.addLast("C");

        assertEquals("[A, B, C]", list.toString());
        assertEquals("A", list.first());
        assertEquals("C", list.last());
        assertEquals(3, list.size());
    }

    @Test
    void acceptsNullElements() {
        list.addLast(null);

        assertNull(list.first());
        assertEquals(1, list.size());
    }

    // ______________ removing ______________

    @Test
    void removeFirstReturnsElementsFromTheFront() {
        fill("A", "B", "C");

        assertEquals("A", list.removeFirst());
        assertEquals("B", list.removeFirst());
        assertEquals("[C]", list.toString());
        assertEquals(1, list.size());
    }

    @Test
    void removeLastReturnsElementsFromTheBack() {
        fill("A", "B", "C");

        assertEquals("C", list.removeLast());
        assertEquals("B", list.removeLast());
        assertEquals("[A]", list.toString());
        assertEquals(1, list.size());
    }

    @Test
    void removingTheOnlyElementLeavesAnEmptyList() {
        list.addLast("A");

        assertEquals("A", list.removeFirst());
        assertTrue(list.isEmpty());
        assertEquals("[]", list.toString());
    }

    @Test
    void listIsReusableAfterBeingEmptied() {
        fill("A", "B");
        list.removeFirst();
        list.removeLast();

        list.addLast("C");
        list.addFirst("B");

        assertEquals("[B, C]", list.toString());
    }

    // ______________ forward iterator ______________

    @Test
    void forEachVisitsFirstToLast() {
        fill("A", "B", "C");

        List<String> seen = new ArrayList<>();
        for (String s : list) {
            seen.add(s);
        }

        assertEquals(List.of("A", "B", "C"), seen);
    }

    @Test
    void iteratorOnEmptyListHasNoNext() {
        Iterator<String> it = list.iterator();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void iteratorThrowsAfterTheLastElement() {
        fill("A");
        Iterator<String> it = list.iterator();
        it.next();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void iteratorRemoveDeletesTheLastReturnedElement() {
        fill("A", "B", "C");
        Iterator<String> it = list.iterator();

        it.next();          // A
        it.next();          // B
        it.remove();        // removes B

        assertEquals("C", it.next());
        assertEquals("[A, C]", list.toString());
        assertEquals(2, list.size());
    }

    @Test
    void iteratorCanRemoveEveryElement() {
        fill("A", "B", "C");
        Iterator<String> it = list.iterator();

        while (it.hasNext()) {
            it.next();
            it.remove();
        }

        assertTrue(list.isEmpty());
        assertEquals("[]", list.toString());
    }

    @Test
    void iteratorRemoveBeforeNextThrows() {
        fill("A");
        Iterator<String> it = list.iterator();

        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorRemoveTwiceInARowThrows() {
        fill("A", "B");
        Iterator<String> it = list.iterator();
        it.next();
        it.remove();

        assertThrows(IllegalStateException.class, it::remove);
    }

    // ______________ descending iterator ______________

    @Test
    void descendingIteratorVisitsLastToFirst() {
        fill("A", "B", "C");

        List<String> seen = new ArrayList<>();
        Iterator<String> it = list.descendingIterator();
        while (it.hasNext()) {
            seen.add(it.next());
        }

        assertEquals(List.of("C", "B", "A"), seen);
    }

    @Test
    void descendingIteratorOnEmptyListHasNoNext() {
        Iterator<String> it = list.descendingIterator();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void descendingIteratorRemoveDeletesTheLastReturnedElement() {
        fill("A", "B", "C");
        Iterator<String> it = list.descendingIterator();

        it.next();          // C
        it.remove();        // removes C

        assertEquals("B", it.next());
        assertEquals("[A, B]", list.toString());
        assertEquals("B", list.last());
    }

    // ______________ helpers ______________

    private void fill(String... elements) {
        for (String e : elements) {
            list.addLast(e);
        }
    }
}
