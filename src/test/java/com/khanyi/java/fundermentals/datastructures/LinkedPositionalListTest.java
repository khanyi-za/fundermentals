package com.khanyi.java.fundermentals.datastructures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class LinkedPositionalListTest {

    // Declared as the interface type: the tests only rely on the PositionalList contract.
    private PositionalList<String> list;

    @BeforeEach
    void setUp() {
        list = new LinkedPositionalList<>();
    }

    // ______________ empty list ______________

    @Test
    void newListIsEmpty() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    void firstAndLastAreNullWhenEmpty() {
        assertNull(list.first());
        assertNull(list.last());
    }

    // ______________ adding ______________

    @Test
    void addFirstAndAddLastReturnPositionsHoldingTheElement() {
        Position<String> b = list.addFirst("B");
        Position<String> c = list.addLast("C");
        Position<String> a = list.addFirst("A");

        assertEquals("A", a.getElement());
        assertEquals("B", b.getElement());
        assertEquals("C", c.getElement());
        assertEquals("[A, B, C]", list.toString());
        assertEquals(3, list.size());
    }

    @Test
    void singleElementIsBothFirstAndLast() {
        Position<String> a = list.addLast("A");

        assertSame(a, list.first());
        assertSame(a, list.last());
    }

    @Test
    void addBeforeInsertsDirectlyBefore() {
        Position<String> c = list.addLast("C");
        list.addFirst("A");

        Position<String> b = list.addBefore(c, "B");

        assertEquals("B", b.getElement());
        assertEquals("[A, B, C]", list.toString());
    }

    @Test
    void addBeforeTheFirstPositionBecomesTheNewFirst() {
        Position<String> b = list.addLast("B");

        Position<String> a = list.addBefore(b, "A");

        assertSame(a, list.first());
    }

    @Test
    void addAfterInsertsDirectlyAfter() {
        Position<String> a = list.addLast("A");
        list.addLast("C");

        Position<String> b = list.addAfter(a, "B");

        assertEquals("B", b.getElement());
        assertEquals("[A, B, C]", list.toString());
    }

    @Test
    void addAfterTheLastPositionBecomesTheNewLast() {
        Position<String> a = list.addLast("A");

        Position<String> b = list.addAfter(a, "B");

        assertSame(b, list.last());
    }

    // ______________ navigating ______________

    @Test
    void beforeAndAfterWalkBetweenNeighbours() {
        Position<String> a = list.addLast("A");
        Position<String> b = list.addLast("B");
        Position<String> c = list.addLast("C");

        assertSame(a, list.before(b));
        assertSame(c, list.after(b));
    }

    @Test
    void beforeFirstAndAfterLastAreNull() {
        list.addLast("A");
        list.addLast("B");

        assertNull(list.before(list.first()));
        assertNull(list.after(list.last()));
    }

    @Test
    void positionsStayValidWhenNeighboursChange() {
        Position<String> b = list.addLast("B");
        list.addFirst("A");
        list.addLast("C");
        list.remove(list.first());

        assertEquals("B", b.getElement());
        assertSame(b, list.first());
    }

    // ______________ set and remove ______________

    @Test
    void setReplacesTheElementAndReturnsTheOldOne() {
        Position<String> p = list.addLast("old");

        assertEquals("old", list.set(p, "new"));
        assertEquals("new", p.getElement());
        assertEquals(1, list.size());
    }

    @Test
    void removeReturnsTheElementAndRelinksNeighbours() {
        Position<String> a = list.addLast("A");
        Position<String> b = list.addLast("B");
        Position<String> c = list.addLast("C");

        assertEquals("B", list.remove(b));
        assertEquals("[A, C]", list.toString());
        assertEquals(2, list.size());
        assertSame(c, list.after(a));
        assertSame(a, list.before(c));
    }

    @Test
    void removingTheOnlyElementLeavesAnEmptyList() {
        Position<String> a = list.addLast("A");

        list.remove(a);

        assertTrue(list.isEmpty());
        assertNull(list.first());
        assertNull(list.last());
    }

    // ______________ invalid positions ______________

    @Test
    void removedPositionCannotBeReadOrUsed() {
        Position<String> a = list.addLast("A");
        list.addLast("B");
        list.remove(a);

        assertThrows(IllegalStateException.class, a::getElement);
        assertThrows(IllegalArgumentException.class, () -> list.after(a));
        assertThrows(IllegalArgumentException.class, () -> list.before(a));
        assertThrows(IllegalArgumentException.class, () -> list.addAfter(a, "X"));
        assertThrows(IllegalArgumentException.class, () -> list.set(a, "X"));
        assertThrows(IllegalArgumentException.class, () -> list.remove(a));
    }

    @Test
    void positionFromAnotherListIsRejected() {
        PositionalList<String> other = new LinkedPositionalList<>();
        Position<String> foreign = other.addLast("X");
        list.addLast("A");

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> list.remove(foreign));
        assertEquals("Position belongs to a different list", ex.getMessage());
        assertEquals(1, other.size());
    }

    @Test
    void positionNotCreatedByALinkedPositionalListIsRejected() {
        Position<String> fake = () -> "fake";

        assertThrows(IllegalArgumentException.class, () -> list.after(fake));
    }

    @Test
    void nullPositionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> list.addAfter(null, "X"));
    }

    // ______________ iteration ______________

    @Test
    void forEachVisitsElementsFirstToLast() {
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");

        List<String> seen = new ArrayList<>();
        for (String s : list) {
            seen.add(s);
        }

        assertEquals(List.of("A", "B", "C"), seen);
    }

    @Test
    void positionsVisitsEveryPositionInOrder() {
        Position<String> a = list.addLast("A");
        Position<String> b = list.addLast("B");

        List<Position<String>> seen = new ArrayList<>();
        for (Position<String> p : list.positions()) {
            seen.add(p);
        }

        assertEquals(2, seen.size());
        assertSame(a, seen.get(0));
        assertSame(b, seen.get(1));
    }

    @Test
    void iteratorsOnEmptyListHaveNoNext() {
        Iterator<String> elements = list.iterator();
        Iterator<Position<String>> positions = list.positions().iterator();

        assertFalse(elements.hasNext());
        assertFalse(positions.hasNext());
        assertThrows(NoSuchElementException.class, elements::next);
        assertThrows(NoSuchElementException.class, positions::next);
    }

    @Test
    void elementIteratorRemoveDeletesTheLastReturnedElement() {
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");
        Iterator<String> it = list.iterator();

        it.next();          // A
        it.next();          // B
        it.remove();        // removes B

        assertEquals("C", it.next());
        assertEquals("[A, C]", list.toString());
    }

    @Test
    void positionIteratorCanRemoveEveryPosition() {
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");
        Iterator<Position<String>> it = list.positions().iterator();

        while (it.hasNext()) {
            it.next();
            it.remove();
        }

        assertTrue(list.isEmpty());
    }

    @Test
    void iteratorRemoveBeforeNextThrows() {
        list.addLast("A");

        assertThrows(IllegalStateException.class, () -> list.iterator().remove());
    }

    @Test
    void iteratorRemoveTwiceInARowThrows() {
        list.addLast("A");
        list.addLast("B");
        Iterator<String> it = list.iterator();
        it.next();
        it.remove();

        assertThrows(IllegalStateException.class, it::remove);
    }
}
