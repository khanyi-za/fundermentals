package com.khanyi.java.fundermentals.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

/**
 * A PositionalList backed by a doubly linked list with header and trailer sentinels.
 * Each node is handed to callers as a Position, so they can insert, replace or
 * remove relative to it in O(1), without searching or using indices.
 */
public class LinkedPositionalList<E> implements PositionalList<E> {

    /** a node is the position. it remembers its owner hence foreigns can be rejected */
    private static class Node<E> implements Position<E> {
        private E element;
        private Node<E> prev;
        private Node<E> next;
        private final LinkedPositionalList<E> owner;

        Node(E element, Node<E> prev, Node<E> next, LinkedPositionalList<E> owner) {
            this.element = element;
            this.prev = prev;
            this.next = next;
            this.owner = owner;
        }

        @Override
        public E getElement() throws IllegalStateException {
            if (next == null) { // removed nodes have next set to null
                throw new IllegalStateException("Position is no longer valid");
            }
            return element;
        }
    }

    private final Node<E> header;
    private final Node<E> trailer;
    private int size = 0;
     public LinkedPositionalList() {
         header = new Node<>(null, null,null,this);
         trailer = new Node<>(null, header, null, this);
         header.next = trailer;
     }

     // ___________________Accessors / Getters___________________

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Position<E> first() {
        return position(header.next);
    }

    @Override
    public Position<E> last() {
        return position(trailer.prev);
    }

    @Override
    public Position<E> before(Position<E> p) throws IllegalArgumentException {
        Node<E> node = validate(p);
        return position(node.prev);
    }

    @Override
    public Position<E> after(Position<E> p) throws IllegalArgumentException {
        Node<E> node = validate(p);
        return position(node.next);
    }

    //_________________ updates / setters ____________

    @Override
    public Position<E> addFirst(E e) {
         return addBetween(e, header, header.next);
    }

    @Override
    public Position<E> addLast(E e) {
         return addBetween(e, trailer.prev, trailer);
    }

    @Override
    public Position<E> addBefore(Position<E> p, E e) throws IllegalArgumentException {
        Node<E> node = validate(p);
        return addBetween(e, node.prev, node);
    }

    @Override
    public Position<E> addAfter(Position<E> p, E e) throws IllegalArgumentException {
        Node<E> node = validate(p);
        return addBetween(e, node, node.next);
    }

    @Override
    public E set(Position<E> p, E e) throws IllegalArgumentException {
        Node<E> node = validate(p);
        E old = node.element;
        node.element = e;
        return old;
    }

    @Override
    public E remove(Position<E> p) throws IllegalArgumentException {
        Node<E> node = validate(p);
        Node<E> predecessor = node.prev;
        Node<E> successor = node.next;
        predecessor.next = successor;
        successor.prev = predecessor;
        size--;

        E element = node.element;
        node.element = null;
        node.prev = null;
        node.next = null;   // marks the position as invalid
        return element;
    }

    //_______________ iteration _________________________

    /** Iterates over elements. Enables: for (E e : list) */
    @Override
    public Iterator<E> iterator() {
        return new ElementIterator();
    }

    /** Iterates over positions. Enables: for (Position<E> p : list.positions()) */
    @Override
    public Iterable<Position<E>> positions() {
        return () -> new PositionIterator();
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (E e : this) {
            joiner.add(String.valueOf(e));
        }
        return joiner.toString();
    }
    // -----------------private helpers -----------------------------------

    /** Checks p is a live node from THIS list, and returns it as a Node. */
    private Node<E> validate(Position<E> p) throws IllegalArgumentException {
        if (!(p instanceof Node<E> node)) {
            throw new IllegalArgumentException("Invalid position");
        }
        if (node.next == null) {
            throw new IllegalArgumentException("Position is no longer in the list");
        }
        if (node.owner != this) {
            throw new IllegalArgumentException("Position belongs to a different list");
        }
        return node;
    }

    /** Hides the sentinels: callers get null instead of header/trailer. */
    private Position<E> position(Node<E> node) {
        if (node == header || node == trailer) {
            return null;
        }
        return node;
    }

    private Position<E> addBetween(E e, Node<E> predecessor, Node<E> successor) {
        Node<E> newest = new Node<>(e, predecessor, successor, this);
        predecessor.next = newest;
        successor.prev = newest;
        size++;
        return newest;
    }
    /** Walks positions from first to last; supports remove(). */
    private class PositionIterator implements Iterator<Position<E>> {
        private Position<E> cursor = first();
        private Position<E> recent = null;

        @Override
        public boolean hasNext() {
            return cursor != null;
        }

        @Override
        public Position<E> next() {
            if (cursor == null) {
                throw new NoSuchElementException("No more positions");
            }
            recent = cursor;
            cursor = after(cursor);
            return recent;
        }

        @Override
        public void remove() {
            if (recent == null) {
                throw new IllegalStateException("Call next() before remove()");
            }
            LinkedPositionalList.this.remove(recent);
            recent = null;
        }
    }
    /** Adapts PositionIterator to return elements instead of positions. */
    private class ElementIterator implements Iterator<E> {
        private final Iterator<Position<E>> positionIterator = new PositionIterator();

        @Override
        public boolean hasNext() {
            return positionIterator.hasNext();
        }

        @Override
        public E next() {
            return positionIterator.next().getElement();
        }

        @Override
        public void remove() {
            positionIterator.remove();
        }
    }

}

