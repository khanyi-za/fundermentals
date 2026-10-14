package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;

/** Removes the item at an index. It remembers what it removed, so undo can put it back. */
public final class RemoveItemEdit implements PlaylistEdit {

    private final Playlist playlist;
    private final int index;
    private MediaItem removed;   // null until execute() has run

    public RemoveItemEdit(Playlist playlist, int index) {
        if (playlist == null) {
            throw new IllegalArgumentException("playlist must not be null");
        }
        this.playlist = playlist;
        this.index = index;
    }

    @Override
    public void execute() {
        removed = playlist.removeAt(index);
    }

    @Override
    public void undo() {
        if (removed == null) {
            throw new IllegalStateException("Nothing to put back: execute() has not run");
        }
        playlist.insert(index, removed);
    }

    @Override
    public String description() {
        return removed == null
                ? "remove item at " + index
                : "remove '" + removed.getTitle() + "' from " + index;
    }
}
