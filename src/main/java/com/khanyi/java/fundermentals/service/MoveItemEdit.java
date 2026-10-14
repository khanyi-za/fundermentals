package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.Playlist;

/** Moves an item so it ends up at another index. Undo is the same move, backwards. */
public final class MoveItemEdit implements PlaylistEdit{

    private final Playlist playlist;
    private final int from;
    private final int to;

    public MoveItemEdit(Playlist playlist, int from, int to) {
        if (playlist == null) {
            throw new IllegalArgumentException("playlist must not be null");
        }
        this.playlist = playlist;
        this.from = from;
        this.to = to;
    }

    @Override
    public void execute() {
        playlist.move(from, to);
    }

    @Override
    public void undo() {
        playlist.move(to, from);
    }

    @Override
    public String description() {
        return "move item " + from + " to " + to;
    }

}
