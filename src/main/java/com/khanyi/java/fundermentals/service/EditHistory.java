package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.datastructures.LinkedStack;
import com.khanyi.java.fundermentals.datastructures.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Undo and redo for one playlist, using two stacks.
 *
 *   perform(edit): run it, push it on the undo stack, and empty the redo stack
 *   undo():        pop from undo, reverse it, push it on redo
 *   redo():        pop from redo, run it again, push it on undo
 *
 * The most recent edit is always on top, which is exactly the order undo needs (LIFO).
 */
public class EditHistory {

    private static final Logger log = LoggerFactory.getLogger(EditHistory.class);

    private final Stack<PlaylistEdit> undoStack = new LinkedStack<>();
    private final Stack<PlaylistEdit> redoStack = new LinkedStack<>();

    /**
     * Runs the edit and remembers it. If the edit throws (e.g. a bad index),
     * the exception goes to the caller and nothing is recorded.
     */
    public void perform(PlaylistEdit edit) {
        if (edit == null) {
            throw new IllegalArgumentException("edit must not be null");
        }
        edit.execute();
        undoStack.push(edit);
        clearRedo();   // a new edit starts a new branch: the old "future" no longer makes sense
        log.debug("Did: {}", edit.description());
    }

    /** @return the edit that was undone */
    public PlaylistEdit undo() {
        if (!canUndo()) {
            throw new IllegalStateException("Nothing to undo");
        }
        PlaylistEdit edit = undoStack.peek();
        edit.undo();             // if this throws, the edit stays on the undo stack
        undoStack.pop();
        redoStack.push(edit);
        log.debug("Undid: {}", edit.description());
        return edit;
    }

    /** @return the edit that was redone */
    public PlaylistEdit redo() {
        if (!canRedo()) {
            throw new IllegalStateException("Nothing to redo");
        }
        PlaylistEdit edit = redoStack.peek();
        edit.execute();
        redoStack.pop();
        undoStack.push(edit);
        log.debug("Redid: {}", edit.description());
        return edit;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** What undo() would reverse, e.g. for an "Undo add 'Comedy' at 3" button. */
    public Optional<String> nextUndo() {
        return canUndo() ? Optional.of(undoStack.peek().description()) : Optional.empty();
    }

    /** What redo() would do again. */
    public Optional<String> nextRedo() {
        return canRedo() ? Optional.of(redoStack.peek().description()) : Optional.empty();
    }

    /** Forgets everything, e.g. after a smart playlist is refreshed and the old indexes mean nothing. */
    public void clear() {
        while (!undoStack.isEmpty()) {
            undoStack.pop();
        }
        clearRedo();
    }

    private void clearRedo() {
        while (!redoStack.isEmpty()) {
            redoStack.pop();
        }
    }
}
