package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.Playlist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.khanyi.java.fundermentals.service.EditTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

public class MoveItemEditTest {

    @ParameterizedTest(name = "move {0} -> {1}: abcde -> {2}, and undo")
    @CsvSource({
            "0, 4, bcdea",
            "4, 0, eabcd",
            "1, 3, acdbe",
            "3, 1, adbce",
            "2, 2, abcde"
    })
    void executeMovesAndUndoMovesBack(int from, int to, String moved) {
        Playlist p = playlistOf("a", "b", "c", "d", "e");
        MoveItemEdit edit = new MoveItemEdit(p, from, to);

        edit.execute();
        assertEquals(moved, ids(p));
        edit.undo();
        assertEquals("abcde", ids(p));
    }

    @Test
    void canBeRedoneByExecutingAgain() {
        Playlist p = playlistOf("a", "b", "c");
        MoveItemEdit edit = new MoveItemEdit(p, 0, 2);

        edit.execute();
        edit.undo();
        edit.execute();

        assertEquals("bca", ids(p));
    }

    @Test
    void movingDuplicatesKeepsTheRightObjects() {
        Playlist p = playlistOf("a", "b");
        p.add(p.get(0));   // "aba": the same track twice
        MoveItemEdit edit = new MoveItemEdit(p, 2, 0);

        edit.execute();
        assertEquals("aab", ids(p));
        edit.undo();

        assertEquals("aba", ids(p));
    }

    @Test
    void badIndexFailsWhenExecuted() {
        Playlist p = playlistOf("a", "b");
        MoveItemEdit edit = new MoveItemEdit(p, 0, 2);

        assertThrows(IndexOutOfBoundsException.class, edit::execute);
        assertEquals("ab", ids(p));
    }

    @Test
    void descriptionNamesBothIndexes() {
        assertEquals("move item 0 to 4", new MoveItemEdit(playlistOf(), 0, 4).description());
    }

    @Test
    void nullPlaylistIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MoveItemEdit(null, 0, 1));
    }
}
