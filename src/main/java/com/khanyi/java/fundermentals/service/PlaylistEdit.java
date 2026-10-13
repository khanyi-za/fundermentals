package com.khanyi.java.fundermentals.service;

/**
 * One change to a playlist that knows how to reverse itself: the Command pattern.
 *
 * Each edit remembers which playlist it changes and everything needed to put it back.
 * EditHistory only ever sees this interface, so it can undo any kind of edit,
 * including ones added later, without changing.
 */
public interface PlaylistEdit {

    /** Makes the change. Called once when the edit is first done, and again on every redo. */
    void execute();

    /** Puts the playlist back exactly as it was before execute(). */
    void undo();

    /** A short human-readable summary, e.g. "add 'Comedy' at 3", for logs and an "Undo ..." button. */
    String description();
}
