package com.khanyi.java.fundermentals.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.SmartPlaylist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.khanyi.java.fundermentals.service.EditTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

public class EditHistoryTest {

    private Playlist playlist;
    private EditHistory history;

    // Captures DEBUG logs from EditHistory (normally hidden), so tests can check them
    private final Logger historyLog = (Logger) LoggerFactory.getLogger(EditHistory.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        playlist = playlistOf("a", "b", "c");
        history = new EditHistory();
        previousLevel = historyLog.getLevel();
        historyLog.setLevel(Level.DEBUG);
        logs.start();
        historyLog.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        historyLog.detachAppender(logs);
        historyLog.setLevel(previousLevel);
    }

    // ______________ a new history ______________

    @Test
    void newHistoryHasNothingToUndoOrRedo() {
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertEquals(Optional.empty(), history.nextUndo());
        assertEquals(Optional.empty(), history.nextRedo());
    }

    @Test
    void undoWithNothingToUndoIsRefused() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, history::undo);

        assertEquals("Nothing to undo", ex.getMessage());
    }

    @Test
    void redoWithNothingToRedoIsRefused() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, history::redo);

        assertEquals("Nothing to redo", ex.getMessage());
    }

    // ______________ perform ______________

    @Test
    void performRunsTheEditAndMakesItUndoable() {
        history.perform(new AddItemEdit(playlist, 3, track("d")));

        assertEquals("abcd", ids(playlist));
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
        assertEquals(Optional.of("add 'D' at 3"), history.nextUndo());
    }

    @Test
    void performRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> history.perform(null));
        assertFalse(history.canUndo());
    }

    @Test
    void aFailedEditIsNotRecorded() {
        history.perform(new MoveItemEdit(playlist, 0, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> history.perform(new RemoveItemEdit(playlist, 9)));

        assertEquals(Optional.of("move item 0 to 1"), history.nextUndo(), "the failed edit must not be on top");
        history.undo();
        assertFalse(history.canUndo());
        assertEquals("abc", ids(playlist));
    }

    @Test
    void aFailedEditDoesNotWipeTheRedoStack() {
        history.perform(new MoveItemEdit(playlist, 0, 2));
        history.undo();

        assertThrows(IndexOutOfBoundsException.class, () -> history.perform(new RemoveItemEdit(playlist, 9)));

        assertTrue(history.canRedo(), "nothing happened, so the future is still valid");
    }

    @Test
    void smartPlaylistRefusalIsNotRecorded() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);

        assertThrows(IllegalArgumentException.class,
                () -> history.perform(new AddItemEdit(rock, 0, track("x", "pop"))));

        assertFalse(history.canUndo());
    }

    // ______________ undo and redo ______________

    @Test
    void undoReversesTheMostRecentEditFirst() {
        history.perform(new AddItemEdit(playlist, 0, track("x")));   // xabc
        history.perform(new MoveItemEdit(playlist, 0, 3));           // abcx
        history.perform(new RemoveItemEdit(playlist, 1));            // acx

        assertEquals("acx", ids(playlist));
        history.undo();
        assertEquals("abcx", ids(playlist));
        history.undo();
        assertEquals("xabc", ids(playlist));
        history.undo();
        assertEquals("abc", ids(playlist));
        assertFalse(history.canUndo());
    }

    @Test
    void undoReturnsTheEditItUndid() {
        PlaylistEdit edit = new MoveItemEdit(playlist, 0, 2);
        history.perform(edit);

        assertSame(edit, history.undo());
    }

    @Test
    void redoReappliesInTheOrderTheyWereUndone() {
        history.perform(new AddItemEdit(playlist, 0, track("x")));
        history.perform(new RemoveItemEdit(playlist, 3));            // xab
        history.undo();
        history.undo();
        assertEquals("abc", ids(playlist));

        history.redo();
        assertEquals("xabc", ids(playlist));
        history.redo();
        assertEquals("xab", ids(playlist));
        assertFalse(history.canRedo());
    }

    @Test
    void redoReturnsTheEditItRedid() {
        PlaylistEdit edit = new MoveItemEdit(playlist, 0, 2);
        history.perform(edit);
        history.undo();

        assertSame(edit, history.redo());
    }

    @Test
    void undoMovesAnEditToRedoAndBack() {
        history.perform(new MoveItemEdit(playlist, 0, 2));

        history.undo();
        assertFalse(history.canUndo());
        assertTrue(history.canRedo());
        assertEquals(Optional.of("move item 0 to 2"), history.nextRedo());

        history.redo();
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    void undoAllThenRedoAllEndsWhereItStarted() {
        history.perform(new AddItemEdit(playlist, 1, track("x")));
        history.perform(new AddItemEdit(playlist, 0, track("y")));
        history.perform(new MoveItemEdit(playlist, 4, 0));
        history.perform(new RemoveItemEdit(playlist, 2));
        String after = ids(playlist);

        while (history.canUndo()) {
            history.undo();
        }
        assertEquals("abc", ids(playlist));
        while (history.canRedo()) {
            history.redo();
        }

        assertEquals(after, ids(playlist));
    }

    @Test
    void aNewEditAfterUndoClearsRedo() {
        history.perform(new MoveItemEdit(playlist, 0, 2));
        history.perform(new MoveItemEdit(playlist, 0, 1));
        history.undo();
        history.undo();
        assertTrue(history.canRedo());

        history.perform(new AddItemEdit(playlist, 0, track("x")));

        assertFalse(history.canRedo());
        assertEquals(Optional.empty(), history.nextRedo());
        assertThrows(IllegalStateException.class, history::redo);
    }

    @Test
    void redoAfterRemoveUsesTheSameEditObject() {
        // RemoveItemEdit remembers what it removed; redo sets it again
        history.perform(new RemoveItemEdit(playlist, 0));
        history.undo();
        history.redo();
        history.undo();

        assertEquals("abc", ids(playlist));
    }

    // ______________ an undo that fails ______________

    @Test
    void anUndoThatThrowsStaysOnTheUndoStack() {
        PlaylistEdit broken = new PlaylistEdit() {
            private boolean firstTry = true;

            @Override
            public void execute() {
            }

            @Override
            public void undo() {
                if (firstTry) {
                    firstTry = false;
                    throw new IllegalStateException("not this time");
                }
            }

            @Override
            public String description() {
                return "broken";
            }
        };
        history.perform(broken);

        assertThrows(IllegalStateException.class, history::undo);

        assertEquals(Optional.of("broken"), history.nextUndo(), "peek, then pop: the edit isn't lost");
        assertFalse(history.canRedo());
        assertSame(broken, history.undo(), "the second try works");
    }

    // ______________ clear ______________

    @Test
    void clearForgetsBothStacksButKeepsThePlaylist() {
        history.perform(new MoveItemEdit(playlist, 0, 2));
        history.perform(new MoveItemEdit(playlist, 0, 1));
        history.undo();

        history.clear();

        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertEquals("bca", ids(playlist));
    }

    @Test
    void historyStillWorksAfterClear() {
        history.perform(new MoveItemEdit(playlist, 0, 2));
        history.clear();

        history.perform(new RemoveItemEdit(playlist, 0));
        history.undo();

        assertEquals("bca", ids(playlist));
    }

    // ______________ separate histories ______________

    @Test
    void eachPlaylistCanHaveItsOwnHistory() {
        Playlist other = playlistOf("x", "y");
        EditHistory otherHistory = new EditHistory();

        history.perform(new RemoveItemEdit(playlist, 0));
        otherHistory.perform(new MoveItemEdit(other, 0, 1));
        history.undo();

        assertEquals("abc", ids(playlist));
        assertEquals("yx", ids(other), "undoing one playlist must not touch the other");
        assertTrue(otherHistory.canUndo());
    }

    // ______________ many edits ______________

    @Test
    void handlesALongHistory() {
        for (int i = 0; i < 1_000; i++) {
            history.perform(new MoveItemEdit(playlist, i % 3, (i + 1) % 3));
        }
        int undone = 0;
        while (history.canUndo()) {
            history.undo();
            undone++;
        }

        assertEquals(1_000, undone);
        assertEquals("abc", ids(playlist));
    }

    // ______________ logging ______________

    @Test
    void logsEveryDoUndoAndRedoAtDebug() {
        history.perform(new MoveItemEdit(playlist, 0, 2));
        history.undo();
        history.redo();

        List<String> messages = new ArrayList<>();
        for (ILoggingEvent event : logs.list) {
            assertEquals(Level.DEBUG, event.getLevel());
            messages.add(event.getFormattedMessage());
        }
        assertEquals(List.of("Did: move item 0 to 2", "Undid: move item 0 to 2", "Redid: move item 0 to 2"), messages);
    }
}
