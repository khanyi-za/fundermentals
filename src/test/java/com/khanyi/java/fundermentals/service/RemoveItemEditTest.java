package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.SmartPlaylist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.khanyi.java.fundermentals.service.EditTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

public class RemoveItemEditTest {

    @ParameterizedTest(name = "remove at {0}: abcd -> {1}")
    @CsvSource({"0, bcd", "1, acd", "2, abd", "3, abc"})
    void executeRemovesAtTheIndex(int index, String expected) {
        Playlist p = playlistOf("a", "b", "c", "d");

        new RemoveItemEdit(p, index).execute();

        assertEquals(expected, ids(p));
    }

    @ParameterizedTest(name = "remove at {0}, then undo")
    @CsvSource({"0", "1", "2", "3"})
    void undoPutsTheSameItemBackInTheSamePlace(int index) {
        Playlist p = playlistOf("a", "b", "c", "d");
        List<MediaItem> before = p.getItems();
        RemoveItemEdit edit = new RemoveItemEdit(p, index);

        edit.execute();
        edit.undo();

        assertEquals("abcd", ids(p));
        assertSame(before.get(index), p.get(index), "the very same object comes back");
    }

    @Test
    void removingTheOnlyItemAndUndoing() {
        Playlist p = playlistOf("a");
        RemoveItemEdit edit = new RemoveItemEdit(p, 0);

        edit.execute();
        assertTrue(p.isEmpty());
        edit.undo();
        assertEquals("a", ids(p));
    }

    @Test
    void canBeRedoneByExecutingAgain() {
        Playlist p = playlistOf("a", "b", "c");
        RemoveItemEdit edit = new RemoveItemEdit(p, 1);

        edit.execute();
        edit.undo();
        edit.execute();

        assertEquals("ac", ids(p));
        edit.undo();
        assertEquals("abc", ids(p));
    }

    @Test
    void undoBeforeExecuteIsRefused() {
        Playlist p = playlistOf("a", "b");
        RemoveItemEdit edit = new RemoveItemEdit(p, 0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, edit::undo);

        assertEquals("Nothing to put back: execute() has not run", ex.getMessage());
        assertEquals("ab", ids(p));
    }

    @Test
    void badIndexFailsWhenExecuted() {
        Playlist p = playlistOf("a");
        RemoveItemEdit edit = new RemoveItemEdit(p, 1);

        assertThrows(IndexOutOfBoundsException.class, edit::execute);
        assertEquals("a", ids(p));
        assertThrows(IllegalStateException.class, edit::undo, "a failed execute leaves nothing to undo");
    }

    @Test
    void undoOnASmartPlaylistPutsTheTrackBack() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);
        rock.add(track("r1", "rock"));
        rock.add(track("r2", "rock"));
        RemoveItemEdit edit = new RemoveItemEdit(rock, 0);

        edit.execute();
        edit.undo();   // goes through checkCanAdd again, and passes

        assertEquals("r1r2", ids(rock));
    }

    @Test
    void descriptionNamesTheTrackOnceItHasRun() {
        RemoveItemEdit edit = new RemoveItemEdit(playlistOf("a", "b"), 1);

        assertEquals("remove item at 1", edit.description());
        edit.execute();
        assertEquals("remove 'B' from 1", edit.description());
    }

    @Test
    void nullPlaylistIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RemoveItemEdit(null, 0));
    }
}
