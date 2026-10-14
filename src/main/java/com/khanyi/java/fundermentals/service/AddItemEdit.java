package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;

/** Inserts an item at an index. Undo removes what is at that index again. */
public final class AddItemEdit implements PlaylistEdit {

    private final Playlist playlist;
    private final int index;
    private final MediaItem item;

    public AddItemEdit(Playlist playlist, int index, MediaItem item) {
        if (playlist == null || item == null) {
            throw new IllegalArgumentException("playlist and item must not be null");
        }
        this.playlist = playlist;
        this.index = index;
        this.item = item;
    }

    @Override
    public void execute() {
        playlist.insert(index, item);
    }

    @Override
    public void undo() {
        playlist.removeAt(index);
    }

    @Override
    public String description() {
        return "add '" + item.getTitle() + "' at " + index;
    }
}
