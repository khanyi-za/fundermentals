package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.data.InMemoryTrackRepository;
import com.khanyi.java.fundermentals.data.Repository;
import com.khanyi.java.fundermentals.data.SpotifyCsvLoader;
import com.khanyi.java.fundermentals.data.TrackNotFoundException;
import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.PodcastEpisode;
import com.khanyi.java.fundermentals.model.SmartPlaylist;
import com.khanyi.java.fundermentals.model.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.khanyi.java.fundermentals.service.EditTestSupport.ids;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PlaylistServiceTest {

    private InMemoryTrackRepository repo;
    private PlaylistService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryTrackRepository(List.of(
                track("a", "rock", 120, 50),
                track("b", "rock", 125, 90),
                track("c", "pop", 128, 70),
                track("d", "rock", 90, 70),
                track("e", "jazz", 122, 10)));
        service = new PlaylistService(repo, counter());
    }

    // ______________ constructing ______________

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new PlaylistService(null, counter()));
        assertThrows(IllegalArgumentException.class, () -> new PlaylistService(repo, null));
        assertThrows(IllegalArgumentException.class, () -> new PlaylistService(null));
    }

    @Test
    void defaultConstructorMakesUniqueUuidIds() {
        PlaylistService withUuids = new PlaylistService(repo);

        String first = withUuids.createPlaylist("One").getId();
        String second = withUuids.createPlaylist("Two").getId();

        assertNotEquals(first, second);
        assertEquals(36, first.length(), "a UUID looks like 8-4-4-4-12 hex digits");
    }

    @Test
    void worksWithAnyRepositoryImplementation() {
        // the service only depends on the Repository interface
        Repository<Track, String> asInterface = repo;
        PlaylistService s = new PlaylistService(asInterface, counter());
        String id = s.createPlaylist("Mix").getId();

        s.addTrack(id, "a");

        assertEquals("a", ids(s.getPlaylist(id)));
    }

    // ______________ creating and finding ______________

    @Test
    void createPlaylistUsesTheInjectedIdGenerator() {
        Playlist first = service.createPlaylist("Mix");
        Playlist second = service.createPlaylist("Chill");

        assertEquals("p1", first.getId());
        assertEquals("p2", second.getId());
        assertEquals("Mix", first.getName());
        assertTrue(first.isEmpty());
    }

    @Test
    void createPlaylistRejectsABlankName() {
        assertThrows(IllegalArgumentException.class, () -> service.createPlaylist(" "));
        assertTrue(service.allPlaylists().isEmpty());
    }

    @Test
    void aGeneratorThatRepeatsAnIdIsCaught() {
        PlaylistService broken = new PlaylistService(repo, () -> "same");
        broken.createPlaylist("First");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> broken.createPlaylist("Second"));

        assertEquals("idGenerator gave a duplicate id: same", ex.getMessage());
        assertEquals("First", broken.getPlaylist("same").getName(), "the first playlist must not be overwritten");
    }

    @Test
    void getAndFindPlaylist() {
        Playlist p = service.createPlaylist("Mix");

        assertSame(p, service.getPlaylist("p1"));
        assertEquals(Optional.of(p), service.findPlaylist("p1"));
        assertEquals(Optional.empty(), service.findPlaylist("nope"));
    }

    @Test
    void getMissingPlaylistThrowsNoSuchElement() {
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> service.getPlaylist("nope"));

        assertEquals("No playlist with id 'nope'", ex.getMessage());
    }

    @Test
    void allPlaylistsInCreationOrderAndOwnedByTheCaller() {
        service.createPlaylist("One");
        service.createPlaylist("Two");
        service.createPlaylist("Three");

        List<Playlist> all = service.allPlaylists();
        assertEquals(List.of("One", "Two", "Three"), all.stream().map(Playlist::getName).toList());

        all.clear();
        assertEquals(3, service.allPlaylists().size());
    }

    @Test
    void renamePlaylist() {
        service.createPlaylist("Mix");

        service.renamePlaylist("p1", "Better Mix");

        assertEquals("Better Mix", service.getPlaylist("p1").getName());
        assertThrows(NoSuchElementException.class, () -> service.renamePlaylist("nope", "x"));
    }

    @Test
    void deletePlaylist() {
        service.createPlaylist("One");
        service.createPlaylist("Two");

        service.deletePlaylist("p1");

        assertTrue(service.findPlaylist("p1").isEmpty());
        assertEquals(List.of("p2"), service.allPlaylists().stream().map(Playlist::getId).toList());
        assertThrows(NoSuchElementException.class, () -> service.deletePlaylist("p1"));
        assertThrows(NoSuchElementException.class, () -> service.undo("p1"), "its history is gone too");
    }

    // ______________ adding ______________

    @Test
    void addTrackAppendsFromTheLibrary() {
        service.createPlaylist("Mix");

        service.addTrack("p1", "a");
        service.addTrack("p1", "c");

        assertEquals("ac", ids(service.getPlaylist("p1")));
        assertSame(repo.getById("a"), service.getPlaylist("p1").get(0));
    }

    @Test
    void addTrackAtAnIndex() {
        service.createPlaylist("Mix");
        service.addTrack("p1", "a");
        service.addTrack("p1", "b");

        service.addTrack("p1", "c", 1);

        assertEquals("acb", ids(service.getPlaylist("p1")));
    }

    @Test
    void addingTheSameTrackTwiceIsAllowed() {
        service.createPlaylist("Mix");

        service.addTrack("p1", "a");
        service.addTrack("p1", "a");

        assertEquals("aa", ids(service.getPlaylist("p1")));
    }

    @Test
    void addUnknownTrackThrowsTrackNotFoundAndRecordsNothing() {
        service.createPlaylist("Mix");

        TrackNotFoundException ex = assertThrows(TrackNotFoundException.class, () -> service.addTrack("p1", "zzz"));

        assertEquals("zzz", ex.getTrackId());
        assertEquals(Optional.empty(), service.nextUndo("p1"));
    }

    @Test
    void missingPlaylistAndMissingTrackAreBothNoSuchElement() {
        // so the web layer can map one exception type to 404 for both
        service.createPlaylist("Mix");

        assertThrows(NoSuchElementException.class, () -> service.addTrack("nope", "a"));
        assertThrows(NoSuchElementException.class, () -> service.addTrack("p1", "zzz"));
    }

    @Test
    void addTrackAtABadIndexRecordsNothing() {
        service.createPlaylist("Mix");

        assertThrows(IndexOutOfBoundsException.class, () -> service.addTrack("p1", "a", 5));

        assertTrue(service.getPlaylist("p1").isEmpty());
        assertEquals(Optional.empty(), service.nextUndo("p1"));
    }

    @Test
    void addItemAcceptsAPodcastEpisode() {
        service.createPlaylist("Commute");
        PodcastEpisode episode = new PodcastEpisode("ep1", "Pilot", Duration.ofMinutes(30), "The Show", 1);

        service.addTrack("p1", "a");
        service.addItem("p1", episode);

        assertSame(episode, service.getPlaylist("p1").get(1));
        assertEquals(Optional.of("add 'Pilot' at 1"), service.nextUndo("p1"));
    }

    // ______________ removing and moving ______________

    @Test
    void removeItemReturnsWhatWasRemoved() {
        Playlist p = playlistWith("a", "b", "c");

        MediaItem removed = service.removeItem(p.getId(), 1);

        assertEquals("b", removed.getId());
        assertEquals("ac", ids(p));
    }

    @Test
    void removeAtABadIndexRecordsNothing() {
        Playlist p = playlistWith("a");

        assertThrows(IndexOutOfBoundsException.class, () -> service.removeItem(p.getId(), 3));

        assertEquals(Optional.of("add 'A' at 0"), service.nextUndo(p.getId()));
    }

    @Test
    void moveItem() {
        Playlist p = playlistWith("a", "b", "c");

        service.moveItem(p.getId(), 0, 2);

        assertEquals("bca", ids(p));
        assertEquals(Optional.of("move item 0 to 2"), service.nextUndo(p.getId()));
    }

    @Test
    void movingToTheSamePlaceIsNotRecorded() {
        Playlist p = playlistWith("a", "b");

        service.moveItem(p.getId(), 1, 1);

        assertEquals("ab", ids(p));
        assertEquals(Optional.of("add 'B' at 1"), service.nextUndo(p.getId()), "no 'move item 1 to 1' on top");
    }

    @Test
    void movingToTheSamePlaceStillChecksTheIndex() {
        Playlist p = playlistWith("a", "b");

        assertThrows(IndexOutOfBoundsException.class, () -> service.moveItem(p.getId(), 5, 5));
    }

    // ______________ undo and redo ______________

    @Test
    void undoAndRedoEveryKindOfEdit() {
        Playlist p = service.createPlaylist("Mix");
        String id = p.getId();
        service.addTrack(id, "a");
        service.addTrack(id, "b");
        service.addTrack(id, "c");      // abc
        service.moveItem(id, 0, 2);     // bca
        service.removeItem(id, 1);      // ba

        assertEquals("remove 'C' from 1", service.undo(id));
        assertEquals("bca", ids(p));
        assertEquals("move item 0 to 2", service.undo(id));
        assertEquals("abc", ids(p));
        assertEquals("move item 0 to 2", service.redo(id));
        assertEquals("bca", ids(p));
    }

    @Test
    void nextUndoAndNextRedo() {
        Playlist p = playlistWith("a");

        assertEquals(Optional.of("add 'A' at 0"), service.nextUndo(p.getId()));
        assertEquals(Optional.empty(), service.nextRedo(p.getId()));

        service.undo(p.getId());

        assertEquals(Optional.empty(), service.nextUndo(p.getId()));
        assertEquals(Optional.of("add 'A' at 0"), service.nextRedo(p.getId()));
    }

    @Test
    void undoWithNothingToUndo() {
        service.createPlaylist("Mix");

        assertThrows(IllegalStateException.class, () -> service.undo("p1"));
        assertThrows(IllegalStateException.class, () -> service.redo("p1"));
    }

    @Test
    void undoOnAMissingPlaylistSaysWhichPlaylist() {
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> service.undo("nope"));

        assertEquals("No playlist with id 'nope'", ex.getMessage());
        assertThrows(NoSuchElementException.class, () -> service.redo("nope"));
        assertThrows(NoSuchElementException.class, () -> service.nextUndo("nope"));
        assertThrows(NoSuchElementException.class, () -> service.nextRedo("nope"));
    }

    @Test
    void eachPlaylistHasItsOwnHistory() {
        Playlist one = playlistWith("a");
        Playlist two = playlistWith("b", "c");

        service.undo(one.getId());

        assertEquals("", ids(one));
        assertEquals("bc", ids(two));
        assertEquals(Optional.of("add 'C' at 1"), service.nextUndo(two.getId()));
    }

    @Test
    void renameIsNotUndoable() {
        Playlist p = playlistWith("a");

        service.renamePlaylist(p.getId(), "New name");

        assertEquals(Optional.of("add 'A' at 0"), service.nextUndo(p.getId()));
    }

    // ______________ smart playlists ______________

    @Test
    void createGenrePlaylistIsFilledMostPopularFirst() {
        SmartPlaylist rock = service.createGenrePlaylist("Rock", "rock", 2);

        assertEquals("p1", rock.getId());
        assertEquals("bd", ids(rock));   // popularity 90 and 70 beat "a" at 50
        assertSame(rock, service.getPlaylist("p1"));
    }

    @Test
    void createTempoPlaylistIsFilledMostPopularFirst() {
        SmartPlaylist gym = service.createTempoPlaylist("Gym", 120, 125, 10);

        assertEquals("bae", ids(gym));   // 125/90, 120/50, 122/10; "c" at 128 and "d" at 90 are out
    }

    @Test
    void creatingASmartPlaylistDoesNotReorderTheLibrary() {
        service.createGenrePlaylist("Rock", "rock", 10);

        assertEquals(List.of("a", "b", "c", "d", "e"), repo.findAll().stream().map(Track::getId).toList());
    }

    @Test
    void smartPlaylistsStartWithNothingToUndo() {
        SmartPlaylist rock = service.createGenrePlaylist("Rock", "rock", 10);

        assertEquals(Optional.empty(), service.nextUndo(rock.getId()));
    }

    @Test
    void smartPlaylistRuleAppliesToServiceEdits() {
        SmartPlaylist rock = service.createGenrePlaylist("Rock", "rock", 10);

        assertThrows(IllegalArgumentException.class, () -> service.addTrack(rock.getId(), "c"));   // pop

        assertEquals(Optional.empty(), service.nextUndo(rock.getId()));
    }

    @Test
    void refreshPicksUpNewTracksAndClearsHistory() {
        SmartPlaylist rock = service.createGenrePlaylist("Rock", "rock", 2);
        service.removeItem(rock.getId(), 0);
        assertTrue(service.nextUndo(rock.getId()).isPresent());
        repo.save(track("f", "rock", 100, 100));   // a new, very popular rock track

        int count = service.refreshSmartPlaylist(rock.getId());

        assertEquals(2, count);
        assertEquals("fb", ids(rock));
        assertEquals(Optional.empty(), service.nextUndo(rock.getId()));
        assertEquals(Optional.empty(), service.nextRedo(rock.getId()));
    }

    @Test
    void refreshingANormalPlaylistIsRefused() {
        service.createPlaylist("Mix");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.refreshSmartPlaylist("p1"));

        assertEquals("Playlist 'Mix' is not a smart playlist", ex.getMessage());
    }

    @Test
    void refreshingAMissingPlaylist() {
        assertThrows(NoSuchElementException.class, () -> service.refreshSmartPlaylist("nope"));
    }

    @Test
    void badSmartPlaylistArgumentsCreateNothing() {
        assertThrows(IllegalArgumentException.class, () -> service.createGenrePlaylist("Rock", " ", 10));
        assertThrows(IllegalArgumentException.class, () -> service.createTempoPlaylist("Gym", 130, 120, 10));
        assertThrows(IllegalArgumentException.class, () -> service.createGenrePlaylist("Rock", "rock", 0));

        assertTrue(service.allPlaylists().isEmpty());
    }

    // ______________ with the real dataset ______________

    @Test
    void worksWithTheRealDatasetIfPresent() throws Exception {
        Path real = Path.of("data", "spotify_tracks.csv");
        assumeTrue(Files.exists(real), "data/spotify_tracks.csv not downloaded; skipping");
        PlaylistService realService = new PlaylistService(
                new InMemoryTrackRepository(new SpotifyCsvLoader().load(real)), counter());

        SmartPlaylist gym = realService.createTempoPlaylist("Gym", 120, 130, 25);
        Playlist mine = realService.createPlaylist("Mine");
        realService.addTrack(mine.getId(), "5SuOikwiRyPMVoIQDJUgSV");   // "Comedy"
        realService.addTrack(mine.getId(), "3nqQXoyQOWXiESFLlDF1hG", 0); // "Unholy" at the top

        assertEquals(25, gym.size());
        assertEquals("Unholy (feat. Kim Petras)", mine.get(0).getTitle());
        realService.undo(mine.getId());
        assertEquals("Comedy", mine.get(0).getTitle());
    }

    // ______________ helpers ______________

    /** Ids "p1", "p2", ... so tests can name playlists without guessing UUIDs. */
    private static Supplier<String> counter() {
        AtomicInteger next = new AtomicInteger();
        return () -> "p" + next.incrementAndGet();
    }

    private Playlist playlistWith(String... trackIds) {
        Playlist p = service.createPlaylist("Test");
        for (String trackId : trackIds) {
            service.addTrack(p.getId(), trackId);
        }
        return p;
    }

    private static Track track(String id, String genre, double tempo, int popularity) {
        return new Track.Builder()
                .id(id)
                .title(id.toUpperCase())
                .artists("Artist")
                .album("Album")
                .genre(genre)
                .tempo(tempo)
                .popularity(popularity)
                .duration(Duration.ofSeconds(60))
                .build();
    }
}
