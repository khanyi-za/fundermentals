package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.SmartPlaylist;
import com.khanyi.java.fundermentals.model.Track;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static com.khanyi.java.fundermentals.service.EditTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

public class AddItemEditTest {

    @ParameterizedTest(name = "add x at {0}: abc -> {1}")
    @CsvSource({"0, xabc", "1, axbc", "2, abxc", "3, abcx"})
    void executeInsertsAtTheIndex(int index, String expected) {
        Playlist p = playlistOf("a", "b", "c");

        new AddItemEdit(p, index, track("x")).execute();

        assertEquals(expected, ids(p));
    }

    @ParameterizedTest(name = "add x at {0}, then undo")
    @CsvSource({"0", "1", "2", "3"})
    void undoPutsThePlaylistBack(int index) {
        Playlist p = playlistOf("a", "b", "c");
        AddItemEdit edit = new AddItemEdit(p, index, track("x"));

        edit.execute();
        edit.undo();

        assertEquals("abc", ids(p));
    }

    @Test
    void addingToAnEmptyPlaylistAndUndoing() {
        Playlist p = playlistOf();
        AddItemEdit edit = new AddItemEdit(p, 0, track("x"));

        edit.execute();
        assertEquals("x", ids(p));
        edit.undo();
        assertTrue(p.isEmpty());
    }

    @Test
    void undoRemovesTheRightCopyOfADuplicate() {
        Playlist p = playlistOf("a", "b");
        Track a = (Track) p.get(0);
        AddItemEdit edit = new AddItemEdit(p, 2, a);   // a second "a" at the end

        edit.execute();
        assertEquals("aba", ids(p));
        edit.undo();

        assertEquals("ab", ids(p));
        assertSame(a, p.get(0), "the original first copy must stay");
    }

    @Test
    void canBeRedoneByExecutingAgain() {
        Playlist p = playlistOf("a", "b");
        AddItemEdit edit = new AddItemEdit(p, 1, track("x"));

        edit.execute();
        edit.undo();
        edit.execute();

        assertEquals("axb", ids(p));
    }

    @Test
    void badIndexFailsWhenExecutedNotWhenCreated() {
        Playlist p = playlistOf("a");
        AddItemEdit edit = new AddItemEdit(p, 5, track("x"));   // creating it is fine

        assertThrows(IndexOutOfBoundsException.class, edit::execute);
        assertEquals("a", ids(p));
    }

    @Test
    void smartPlaylistRuleStillApplies() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);
        AddItemEdit pop = new AddItemEdit(rock, 0, track("x", "pop"));
        AddItemEdit rockTrack = new AddItemEdit(rock, 0, track("y", "rock"));

        assertThrows(IllegalArgumentException.class, pop::execute);
        rockTrack.execute();

        assertEquals("y", ids(rock));
    }

    @Test
    void descriptionNamesTheTitleAndIndex() {
        assertEquals("add 'X' at 2", new AddItemEdit(playlistOf(), 2, track("x")).description());
    }

    @Test
    void nullPlaylistOrItemIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AddItemEdit(null, 0, track("x")));
        assertThrows(IllegalArgumentException.class, () -> new AddItemEdit(playlistOf(), 0, null));
    }

    @Test
    void isAPlaylistEdit() {
        PlaylistEdit edit = new AddItemEdit(playlistOf(), 0, track("x"));

        assertInstanceOf(PlaylistEdit.class, edit);
    }
}
