package com.khanyi.java.fundermentals.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A generic doubly linked list using header and trailer sentinel nodes.
 * so adding and removing never need special cases for the ends of the list.
 */
public class DoublyLinkedList<E> implements Iterable<E> {

    /**
     * one node on the list/chain. static avoids creating unnecessary association with an instance of this class
     * a node only needs to know prev and next.
     */
    private static class Node<E> {
        private E element;
        private Node<E> prev;
        private Node<E> next;

        Node(E element, Node<E> prev, Node<E> next) {
            this.element = element;
            this.prev = prev;
            this.next = next;
        }
    }

    private final Node<E> header;
    private final Node<E> trailer;
    private int size = 0;

    public DoublyLinkedList() {
        header = new Node<>(null, null, null);
        trailer = new Node<>(null, header, null);
        header.next = trailer;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public E first() {
        requireNotEmpty();
        return header.next.element;
    }

    public E last() {
        requireNotEmpty();
        return trailer.prev.element;
    }

    public void addFirst(E e) {
        addBetween(e, header, header.next);
    }

    public void addLast(E e) {
        addBetween(e, trailer.prev, trailer);
    }

    public E removeFirst() {
        requireNotEmpty();
        return unlink(header.next);
    }

    public E removeLast() {
        requireNotEmpty();
        return unlink(trailer.prev);
    }

    /** Iterates from first to last. Enables: for (E e : list) */
    @Override
    public Iterator<E> iterator() {
        return new NodeIterator(true);
    }

    /** Iterates from last to first. */
    public Iterator<E> descendingIterator() {
        return new NodeIterator(false);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<E> walk = header.next;
        while (walk != trailer) {
            sb.append(walk.element);
            walk = walk.next;
            if (walk != trailer) {
                sb.append(", ");
            }
        }
        return sb.append("]").toString();
    }

    // _______________ PRIVATE HELPERS __________________

    private void addBetween(E e, Node<E> predecessor, Node<E> successor) {
        Node<E> newest = new Node<>(e, predecessor, successor);
        predecessor.next = newest;
        successor.prev = newest;
        size++;
    }

    private E unlink(Node<E> node) {
        Node<E> predecessor = node.prev;
        Node<E> successor = node.next;
        predecessor.next = successor;
        successor.prev = predecessor;
        size--;

        E element = node.element;
        node.element = null; // dropping the references so that the garbage collector can reclaim them
        node.prev =null;
        node.next = null;
        return element;
    }

    private void requireNotEmpty() {
        if (isEmpty()) {
            throw new EmptyCollectionException("List is empty");
        }
    }

    /**
     * Walks the nodes in either direction
     * not static, it needs association with an instance of this list for the header and trailer
     */
    private class NodeIterator implements Iterator<E> {
        private final boolean forward;
        private Node<E> cursor;                 // the node next() will return
        private Node<E> lastReturned = null;    // the node remove() would delete

        NodeIterator(boolean forward) {
            this.forward = forward;
            this.cursor = forward ? header.next : trailer.prev;
        }

        @Override
        public boolean hasNext() {
            return forward ? cursor != trailer : cursor != header;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            lastReturned = cursor;
            cursor = forward ? cursor.next : cursor.prev;
            return lastReturned.element;
        }

        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException("Call next() before remove()");
            }
            unlink(lastReturned);
            lastReturned = null;
        }
    }

}
