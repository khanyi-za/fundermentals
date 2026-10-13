package com.khanyi.java.fundermentals.model;

import com.khanyi.java.fundermentals.datastructures.RepeatingIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlaylistTest {

    private Track a;
    private Track b;
    private Track c;
    private Track d;
    private Track e;
    private Playlist playlist;

    @BeforeEach
    void setUp() {
        a = track("a", 60);
        b = track("b", 120);
        c = track("c", 180);
        d = track("d", 240);
        e = track("e", 300);
        playlist = new Playlist("p1", "Road Trip");
    }

    // ______________ constructing and naming ______________

    @Test
    void newPlaylistIsEmpty() {
        assertEquals("p1", playlist.getId());
        assertEquals("Road Trip", playlist.getName());
        assertEquals(0, playlist.size());
        assertTrue(playlist.isEmpty());
        assertEquals(Duration.ZERO, playlist.getTotalDuration());
        assertFalse(playlist.iterator().hasNext());
    }

    @Test
    void idAndNameAreStripped() {
        Playlist p = new Playlist("  p9 ", "  Chill  ");

        assertEquals("p9", p.getId());
        assertEquals("Chill", p.getName());
    }

    @Test
    void blankIdOrNameIsRejected() {
        assertEquals("id must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new Playlist(" ", "x")).getMessage());
        assertEquals("name must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new Playlist("p", null)).getMessage());
    }

    @Test
    void renameChangesTheNameButNotTheId() {
        playlist.rename("  Long Drive ");

        assertEquals("Long Drive", playlist.getName());
        assertEquals("p1", playlist.getId());
    }

    @Test
    void renameToBlankIsRejectedAndKeepsTheOldName() {
        assertThrows(IllegalArgumentException.class, () -> playlist.rename(""));
        assertEquals("Road Trip", playlist.getName());
    }

    // ______________ adding and inserting ______________

    @Test
    void addAppendsInOrder() {
        fill(a, b, c);

        assertEquals(List.of("a", "b", "c"), ids());
        assertEquals(3, playlist.size());
        assertFalse(playlist.isEmpty());
    }

    @Test
    void duplicatesAreAllowed() {
        fill(a, b, a);

        assertEquals(List.of("a", "b", "a"), ids());
    }

    @Test
    void insertAtTheStartMiddleAndEnd() {
        fill(b, d);

        playlist.insert(0, a);
        playlist.insert(2, c);
        playlist.insert(4, e);   // size() means "at the end"

        assertEquals(List.of("a", "b", "c", "d", "e"), ids());
    }

    @Test
    void insertIntoAnEmptyPlaylist() {
        playlist.insert(0, a);

        assertEquals(List.of("a"), ids());
    }

    @Test
    void insertOutsideZeroToSizeIsRejected() {
        fill(a, b);

        assertThrows(IndexOutOfBoundsException.class, () -> playlist.insert(-1, c));
        assertThrows(IndexOutOfBoundsException.class, () -> playlist.insert(3, c));
        assertEquals(List.of("a", "b"), ids());
    }

    @Test
    void nullItemIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> playlist.add(null));
        assertThrows(IllegalArgumentException.class, () -> playlist.insert(0, null));
        assertTrue(playlist.isEmpty());
    }

    @Test
    void holdsTracksAndPodcastEpisodesTogether() {
        PodcastEpisode episode = new PodcastEpisode("ep1", "Pilot", Duration.ofMinutes(30), "The Show", 1);

        playlist.add(a);
        playlist.add(episode);

        assertSame(episode, playlist.get(1));
        assertEquals(Duration.ofSeconds(60 + 1800), playlist.getTotalDuration());
    }

    // ______________ get, indexOf, contains ______________

    @Test
    void getReturnsTheItemAtEachIndex() {
        fill(a, b, c, d, e);

        for (int i = 0; i < 5; i++) {
            assertEquals(List.of("a", "b", "c", "d", "e").get(i), playlist.get(i).getId());
        }
    }

    @Test
    void getOutsideTheListIsRejectedWithTheStandardMessage() {
        fill(a, b);

        assertThrows(IndexOutOfBoundsException.class, () -> playlist.get(-1));
        IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () -> playlist.get(2));
        assertEquals("Index 2 out of bounds for length 2", ex.getMessage());
    }

    @Test
    void indexOfFindsTheFirstOccurrence() {
        fill(a, b, a);

        assertEquals(0, playlist.indexOf(a));
        assertEquals(1, playlist.indexOf(b));
        assertEquals(-1, playlist.indexOf(c));
        assertEquals(-1, playlist.indexOf(null));
    }

    @Test
    void indexOfUsesEqualsSoAnotherCopyOfTheSameTrackIsFound() {
        playlist.add(a);

        assertEquals(0, playlist.indexOf(track("a", 60)));
        assertTrue(playlist.contains(track("a", 60)));
        assertFalse(playlist.contains(b));
    }

    // ______________ removing ______________

    @Test
    void removeAtReturnsTheRemovedItem() {
        fill(a, b, c);

        assertSame(b, playlist.removeAt(1));
        assertEquals(List.of("a", "c"), ids());
    }

    @Test
    void removeFirstAndLast() {
        fill(a, b, c);

        playlist.removeAt(0);
        playlist.removeAt(1);

        assertEquals(List.of("b"), ids());
    }

    @Test
    void removeOnlyOneOfTwoDuplicates() {
        fill(a, b, a);

        playlist.removeAt(2);

        assertEquals(List.of("a", "b"), ids());
    }

    @Test
    void removeOutsideTheListIsRejected() {
        assertThrows(IndexOutOfBoundsException.class, () -> playlist.removeAt(0));
        fill(a);
        assertThrows(IndexOutOfBoundsException.class, () -> playlist.removeAt(1));
    }

    @Test
    void removeThenInsertAtTheSameIndexRestoresTheList() {
        // this is exactly what RemoveItemEdit.undo() will do
        fill(a, b, c, d);

        MediaItem removed = playlist.removeAt(2);
        playlist.insert(2, removed);

        assertEquals(List.of("a", "b", "c", "d"), ids());
    }

    // ______________ moving ______________

    @ParameterizedTest(name = "move({0}, {1}) gives {2}")
    @CsvSource({
            "0, 4, bcdea",
            "4, 0, eabcd",
            "1, 3, acdbe",
            "3, 1, adbce",
            "0, 1, bacde",
            "2, 2, abcde"
    })
    void moveLeavesTheItemAtTheTargetIndex(int from, int to, String expected) {
        fill(a, b, c, d, e);

        playlist.move(from, to);

        assertEquals(expected, String.join("", ids()));
    }

    @ParameterizedTest(name = "move({0}, {1}) then move({1}, {0})")
    @CsvSource({"0, 4", "4, 0", "1, 3", "3, 1", "2, 3", "0, 0"})
    void moveBackUndoesAMove(int from, int to) {
        // this is exactly what MoveItemEdit.undo() will do
        fill(a, b, c, d, e);

        playlist.move(from, to);
        playlist.move(to, from);

        assertEquals(List.of("a", "b", "c", "d", "e"), ids());
    }

    @Test
    void moveOutsideTheListIsRejected() {
        fill(a, b, c);

        assertThrows(IndexOutOfBoundsException.class, () -> playlist.move(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> playlist.move(-1, 0));
        assertEquals(List.of("a", "b", "c"), ids());
    }

    @Test
    void moveKeepsTheSize() {
        fill(a, b, c);

        playlist.move(0, 2);

        assertEquals(3, playlist.size());
    }

    // ______________ total duration ______________

    @Test
    void totalDurationAddsEveryItemIncludingDuplicates() {
        fill(a, b, a);   // 60 + 120 + 60

        assertEquals(Duration.ofSeconds(240), playlist.getTotalDuration());
    }

    // ______________ snapshots and iteration ______________

    @Test
    void getItemsIsASnapshot() {
        fill(a, b);
        List<MediaItem> snapshot = playlist.getItems();

        playlist.add(c);

        assertEquals(2, snapshot.size());
    }

    @Test
    void getItemsCannotBeChanged() {
        fill(a, b);

        assertThrows(UnsupportedOperationException.class, () -> playlist.getItems().add(c));
        assertThrows(UnsupportedOperationException.class, () -> playlist.getItems().remove(0));
    }

    @Test
    void forEachLoopGoesInOrder() {
        fill(a, b, c);
        List<String> seen = new ArrayList<>();

        for (MediaItem item : playlist) {
            seen.add(item.getId());
        }

        assertEquals(List.of("a", "b", "c"), seen);
    }

    @Test
    void iteratorCannotRemoveBehindThePlaylistsBack() {
        fill(a, b);
        Iterator<MediaItem> it = playlist.iterator();
        it.next();

        assertThrows(UnsupportedOperationException.class, it::remove);
        assertEquals(2, playlist.size());
    }

    @Test
    void changingThePlaylistWhileIteratingIsSafe() {
        fill(a, b, c);
        List<String> seen = new ArrayList<>();

        for (MediaItem item : playlist) {
            seen.add(item.getId());
            playlist.add(d);   // the loop runs over a snapshot, so no ConcurrentModificationException
        }

        assertEquals(List.of("a", "b", "c"), seen);
        assertEquals(6, playlist.size());
    }

    @Test
    void worksWithRepeatingIterator() {
        fill(a, b);
        Iterator<MediaItem> twice = RepeatingIterator.times(playlist, 2);
        List<String> seen = new ArrayList<>();

        twice.forEachRemaining(item -> seen.add(item.getId()));

        assertEquals(List.of("a", "b", "a", "b"), seen);
    }

    // ______________ a bigger list (both walking directions) ______________

    @Test
    void indexesAreRightFromBothEndsOfALongList() {
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            playlist.add(track("t" + i, 60));
            expected.add("t" + i);
        }

        for (int i = 0; i < 101; i++) {
            assertEquals("t" + i, playlist.get(i).getId());
        }
        playlist.move(0, 100);
        playlist.move(100, 0);
        playlist.removeAt(75);
        expected.remove(75);
        playlist.insert(25, track("new", 60));
        expected.add(25, "new");

        assertEquals(expected, ids());
    }

    // ______________ identity and toString ______________

    @Test
    void playlistsAreEqualOnlyToThemselves() {
        Playlist same = new Playlist("p1", "Road Trip");

        assertNotEquals(playlist, same, "mutable objects use identity equality");
        assertEquals(playlist, playlist);
    }

    @Test
    void toStringShowsIdNameAndSize() {
        fill(a, b);

        assertEquals("Playlist[p1: Road Trip, 2 items]", playlist.toString());
    }

    // ______________ the checkCanAdd hook ______________

    @Test
    void subclassHookCanRefuseItemsBeforeAnythingChanges() {
        Playlist noC = new Playlist("p2", "No C") {
            @Override
            protected void checkCanAdd(MediaItem item) {
                if (item.getId().equals("c")) {
                    throw new IllegalArgumentException("no c");
                }
            }
        };
        noC.add(a);
        noC.add(b);

        assertThrows(IllegalArgumentException.class, () -> noC.add(c));
        assertThrows(IllegalArgumentException.class, () -> noC.insert(0, c));
        assertEquals(2, noC.size());
        noC.move(0, 1);   // moving doesn't ask the hook again
        assertEquals("b", noC.get(0).getId());
    }

    // ______________ helpers ______________

    private void fill(MediaItem... items) {
        for (MediaItem item : items) {
            playlist.add(item);
        }
    }

    private List<String> ids() {
        return playlist.getItems().stream().map(MediaItem::getId).toList();
    }

    private static Track track(String id, int seconds) {
        return new Track.Builder()
                .id(id)
                .title("Title " + id)
                .artists("Artist")
                .album("Album")
                .genre("pop")
                .duration(Duration.ofSeconds(seconds))
                .build();
    }
}
