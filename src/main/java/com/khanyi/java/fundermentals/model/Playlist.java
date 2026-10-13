package com.khanyi.java.fundermentals.model;

import com.khanyi.java.fundermentals.datastructures.LinkedPositionalList;
import com.khanyi.java.fundermentals.datastructures.Position;
import com.khanyi.java.fundermentals.datastructures.PositionalList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * An ordered list of tracks and episodes that the user builds and edits.
 *
 * The same item may appear more than once, so items are addressed by index (0 = first),
 * the same way the REST API will address them: DELETE /playlists/{id}/items/{index}.
 *
 * Stored in our own LinkedPositionalList: moving or removing an item relinks two nodes
 * instead of shifting every element after it, the way an ArrayList would.
 */
public class Playlist implements Iterable<MediaItem> {

    private final String id;
    private String name;
    private final PositionalList<MediaItem> items = new LinkedPositionalList<>();

    public Playlist(String id, String name) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
    }

    // ______________ identity ______________

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
    }

    // ______________ reading ______________

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public MediaItem get(int index) {
        Objects.checkIndex(index, items.size());
        return positionAt(index).getElement();
    }

    /** @return the index of the first occurrence of item, or -1 if it isn't here */
    public int indexOf(MediaItem item) {
        int index = 0;
        for (MediaItem current : items) {
            if (current.equals(item)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public boolean contains(MediaItem item) {
        return indexOf(item) >= 0;
    }

    public Duration getTotalDuration() {
        Duration total = Duration.ZERO;
        for (MediaItem item : items) {
            total = total.plus(item.getDuration());
        }
        return total;
    }

    /** A snapshot: later changes to the playlist don't show up in it, and it can't be changed. */
    public List<MediaItem> getItems() {
        List<MediaItem> copy = new ArrayList<>(items.size());
        for (MediaItem item : items) {
            copy.add(item);
        }
        return List.copyOf(copy);
    }

    /** Iterates over a snapshot, so remove() isn't possible: every change goes through the methods below. */
    @Override
    public Iterator<MediaItem> iterator() {
        return getItems().iterator();
    }

    // ______________ changing ______________

    public void add(MediaItem item) {
        insert(items.size(), item);
    }

    /** Inserts so that the item ends up at index. index == size() means "at the end". */
    public void insert(int index, MediaItem item) {
        Objects.checkIndex(index, items.size() + 1);
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }
        checkCanAdd(item);
        insertAt(index, item);
    }

    public MediaItem removeAt(int index) {
        Objects.checkIndex(index, items.size());
        return items.remove(positionAt(index));
    }

    /**
     * Moves the item at from so that it ends up at index to.
     * Moving it back again is simply move(to, from), which is what undo will use.
     */
    public void move(int from, int to) {
        Objects.checkIndex(from, items.size());
        Objects.checkIndex(to, items.size());
        if (from == to) {
            return;
        }
        MediaItem item = items.remove(positionAt(from));
        insertAt(to, item);   // no checkCanAdd: the item was already allowed in
    }

    // ______________ for subclasses ______________

    /**
     * Called before any item is added. Does nothing here; a subclass can override it and
     * throw IllegalArgumentException to refuse items (SmartPlaylist only takes matching tracks).
     */
    protected void checkCanAdd(MediaItem item) {
    }

    // ______________ helpers ______________

    private void insertAt(int index, MediaItem item) {
        if (index == items.size()) {
            items.addLast(item);
        } else {
            items.addBefore(positionAt(index), item);
        }
    }

    /** Walks from whichever end is closer: the list is doubly linked, so both directions are O(1) per step. */
    private Position<MediaItem> positionAt(int index) {
        if (index < items.size() / 2) {
            Position<MediaItem> p = items.first();
            for (int i = 0; i < index; i++) {
                p = items.after(p);
            }
            return p;
        }
        Position<MediaItem> p = items.last();
        for (int i = items.size() - 1; i > index; i--) {
            p = items.before(p);
        }
        return p;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + ": " + name + ", " + items.size() + " items]";
    }
}
