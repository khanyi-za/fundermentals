package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.data.InMemoryTrackRepository;
import com.khanyi.java.fundermentals.data.SpotifyCsvLoader;
import com.khanyi.java.fundermentals.data.TrackNotFoundException;
import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PlayerServiceTest {

    private InMemoryTrackRepository repo;
    private PlaylistService playlists;
    private PlayerService player;
    private String abcd;   // id of a playlist holding a, b, c, d

    @BeforeEach
    void setUp() {
        repo = new InMemoryTrackRepository(List.of(
                track("a", 10), track("b", 20), track("c", 30), track("d", 40),
                track("q", 0), track("r", 0)));
        AtomicInteger next = new AtomicInteger();
        playlists = new PlaylistService(repo, () -> "p" + next.incrementAndGet());
        abcd = playlistOf("a", "b", "c", "d");
        player = new PlayerService(playlists, repo);
    }

    // ______________ constructing and defaults ______________

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerService(null, repo));
        assertThrows(IllegalArgumentException.class, () -> new PlayerService(playlists, null));
    }

    @Test
    void startsIdleWithSensibleDefaults() {
        assertEquals(Optional.empty(), player.nowPlaying());
        assertEquals(Optional.empty(), player.currentPlaylist());
        assertInstanceOf(InOrderStrategy.class, player.getShuffleStrategy());
        assertEquals(RepeatMode.OFF, player.getRepeatMode());
        assertTrue(player.upNext(5).isEmpty());
        assertTrue(player.recentlyPlayed().isEmpty());
        assertEquals(0, player.queueSize());
    }

    @Test
    void nextBeforePlayingGivesNothing() {
        assertEquals(Optional.empty(), player.next());
    }

    // ______________ play ______________

    @Test
    void playStartsAtTheTop() {
        MediaItem first = player.play(abcd);

        assertEquals("a", first.getId());
        assertEquals("a", nowPlaying());
        assertEquals("bcd", ids(player.upNext(10)));
        assertEquals(abcd, player.currentPlaylist().orElseThrow().getId());
    }

    @Test
    void playUnknownPlaylist() {
        assertThrows(NoSuchElementException.class, () -> player.play("nope"));
    }

    @Test
    void playEmptyPlaylistIsRefused() {
        String empty = playlists.createPlaylist("Empty").getId();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> player.play(empty));

        assertEquals("Playlist 'Empty' is empty", ex.getMessage());
        assertEquals(Optional.empty(), player.nowPlaying());
    }

    @Test
    void playUsesASnapshotOfThePlaylist() {
        player.play(abcd);

        playlists.addTrack(abcd, "q");   // edited while playing

        assertEquals("bcd", ids(player.upNext(10)), "the change waits for the next play()");
        player.play(abcd);
        assertEquals("bcdq", ids(player.upNext(10)));
    }

    // ______________ next, end of playlist ______________

    @Test
    void nextWalksThroughThePlaylist() {
        player.play(abcd);

        assertEquals("b", id(player.next()));
        assertEquals("c", id(player.next()));
        assertEquals("d", id(player.next()));
    }

    @Test
    void theEndOfThePlaylistIsEmptyNotAnError() {
        player.play(abcd);
        skip(3);

        assertEquals(Optional.empty(), player.next());
        assertEquals(Optional.empty(), player.nowPlaying());
        assertEquals(Optional.empty(), player.next(), "still nothing after the end");
    }

    @Test
    void trackFinishedMovesOnWhenNotRepeatingOne() {
        player.play(abcd);

        assertEquals("b", id(player.trackFinished()));
    }

    // ______________ recently played and previous ______________

    @Test
    void recentlyPlayedIsMostRecentFirst() {
        player.play(abcd);
        skip(2);   // now on c

        assertEquals("ba", ids(player.recentlyPlayed()));
    }

    @Test
    void lookingAtTheHistoryDoesNotChangeIt() {
        player.play(abcd);
        skip(2);

        player.recentlyPlayed();
        player.recentlyPlayed();

        assertEquals("ba", ids(player.recentlyPlayed()));
        assertEquals("b", player.previous().getId());
    }

    @Test
    void previousGoesBackAndNextReturns() {
        player.play(abcd);
        skip(2);   // a, b played; on c

        assertEquals("b", player.previous().getId());
        assertEquals("a", player.previous().getId());
        assertEquals("bc", ids(player.upNext(2)), "the items stepped back past come first");
        assertEquals("b", id(player.next()));
        assertEquals("c", id(player.next()));
        assertEquals("d", id(player.next()), "then the playlist carries on");
    }

    @Test
    void previousWithNothingBeforeIsRefused() {
        player.play(abcd);

        IllegalStateException ex = assertThrows(IllegalStateException.class, player::previous);

        assertEquals("Nothing was played before this", ex.getMessage());
        assertEquals("a", nowPlaying());
    }

    @Test
    void previousWorksAfterTheEnd() {
        player.play(abcd);
        skip(4);   // past the end
        assertEquals(Optional.empty(), player.nowPlaying());

        assertEquals("d", player.previous().getId());
    }

    // ______________ Play Next queue ______________

    @Test
    void queuedTracksPlayNextInTheOrderTheyWereQueued() {
        player.play(abcd);

        player.playNext("q");
        player.playNext("r");

        assertEquals(2, player.queueSize());
        assertEquals("qrbcd", ids(player.upNext(10)));
        assertEquals("q", id(player.next()));
        assertEquals("r", id(player.next()));
        assertEquals("b", id(player.next()));
        assertEquals(0, player.queueSize());
    }

    @Test
    void lookingAtTheQueueDoesNotChangeIt() {
        player.play(abcd);
        player.playNext("q");
        player.playNext("r");

        player.upNext(10);
        player.upNext(10);

        assertEquals(2, player.queueSize());
        assertEquals("q", id(player.next()));
    }

    @Test
    void queueingAnUnknownTrackFails() {
        TrackNotFoundException ex = assertThrows(TrackNotFoundException.class, () -> player.playNext("zzz"));

        assertEquals("zzz", ex.getTrackId());
        assertEquals(0, player.queueSize());
    }

    @Test
    void queuedTracksPlayEvenAfterThePlaylistEnds() {
        player.play(abcd);
        skip(3);   // on d, the last one
        player.playNext("q");

        assertEquals("q", id(player.next()));
        assertEquals(Optional.empty(), player.next());
    }

    @Test
    void theQueueAndHistorySurviveSwitchingPlaylists() {
        String other = playlistOf("c", "d");
        player.play(abcd);
        player.playNext("q");

        player.play(other);

        assertEquals("c", nowPlaying());
        assertEquals("qd", ids(player.upNext(10)));
        assertEquals("a", ids(player.recentlyPlayed()));
    }

    @Test
    void steppedBackItemsComeBeforeTheQueue() {
        player.play(abcd);
        player.next();             // on b
        player.playNext("q");
        player.previous();         // back on a; b is "forward"

        assertEquals("bqcd", ids(player.upNext(10)));
    }

    @Test
    void playingANewPlaylistForgetsSteppedBackItems() {
        player.play(abcd);
        player.next();
        player.previous();         // b is "forward"

        player.play(abcd);

        assertEquals("bcd", ids(player.upNext(10)));
    }

    // ______________ repeat ______________

    @Test
    void repeatAllWrapsRound() {
        player.setRepeatMode(RepeatMode.ALL);
        player.play(abcd);
        StringBuilder played = new StringBuilder("a");

        for (int i = 0; i < 6; i++) {
            played.append(id(player.next()));
        }

        assertEquals("abcdabc", played.toString());
    }

    @Test
    void repeatAllUpNextWrapsRoundToo() {
        player.setRepeatMode(RepeatMode.ALL);
        player.play(abcd);
        player.next();   // on b

        assertEquals("cdabcdabc", ids(player.upNext(9)));
    }

    @Test
    void repeatOneReplaysWhenTheTrackEnds() {
        player.setRepeatMode(RepeatMode.ONE);
        player.play(abcd);

        assertEquals("a", id(player.trackFinished()));
        assertEquals("a", id(player.trackFinished()));
        assertEquals("aa", ids(player.recentlyPlayed()), "each replay counts as played");
    }

    @Test
    void repeatOneStillLetsTheUserSkip() {
        player.setRepeatMode(RepeatMode.ONE);
        player.play(abcd);

        assertEquals("b", id(player.next()));
    }

    @Test
    void repeatOffStopsAtTheEnd() {
        player.play(abcd);
        skip(3);

        assertEquals(Optional.empty(), player.trackFinished());
    }

    @Test
    void turningRepeatAllOnAtTheEndCarriesOn() {
        player.play(abcd);
        skip(3);   // on d

        player.setRepeatMode(RepeatMode.ALL);

        assertEquals("a", id(player.next()));
    }

    @Test
    void cycleRepeatMode() {
        assertEquals(RepeatMode.ALL, player.cycleRepeatMode());
        assertEquals(RepeatMode.ONE, player.cycleRepeatMode());
        assertEquals(RepeatMode.OFF, player.cycleRepeatMode());
        assertEquals(RepeatMode.OFF, player.getRepeatMode());
    }

    @Test
    void nullRepeatModeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> player.setRepeatMode(null));
    }

    @Test
    void upNextRejectsANegativeCountAndAllowsZero() {
        player.play(abcd);

        assertThrows(IllegalArgumentException.class, () -> player.upNext(-1));
        assertTrue(player.upNext(0).isEmpty());
        assertEquals("bc", ids(player.upNext(2)));
    }

    // ______________ setter injection: shuffle ______________

    @Test
    void shuffleStrategyCanBeInjectedBeforePlaying() {
        player.setShuffleStrategy(reversed());

        assertEquals("d", player.play(abcd).getId());
        assertEquals("cba", ids(player.upNext(10)));
    }

    @Test
    void changingShuffleMidSongKeepsTheCurrentTrackAndReordersTheRest() {
        player.play(abcd);
        player.next();   // on b; a already played

        player.setShuffleStrategy(reversed());

        assertEquals("b", nowPlaying());
        assertEquals("dca", ids(player.upNext(10)), "the whole playlist minus b, in the new order");
    }

    @Test
    void changingShuffleWhileAQueuedTrackPlays() {
        player.play(abcd);
        player.playNext("q");
        player.next();   // on q, which isn't in the playlist

        player.setShuffleStrategy(reversed());

        assertEquals("q", nowPlaying());
        assertEquals("dcba", ids(player.upNext(10)));
    }

    @Test
    void changingShuffleBeforePlayingChangesNothingYet() {
        player.setShuffleStrategy(reversed());

        assertEquals(Optional.empty(), player.nowPlaying());
        assertTrue(player.upNext(10).isEmpty());
    }

    @Test
    void aSeededRandomShuffleIsRepeatable() {
        // two separate players, each given its own Random(42): the same "random" order
        PlayerService first = new PlayerService(playlists, repo);
        PlayerService second = new PlayerService(playlists, repo);
        first.setShuffleStrategy(new RandomShuffle(new Random(42)));
        second.setShuffleStrategy(new RandomShuffle(new Random(42)));

        String firstRun = first.play(abcd).getId() + ids(first.upNext(10));
        String secondRun = second.play(abcd).getId() + ids(second.upNext(10));

        assertEquals(firstRun, secondRun);
        assertEquals(4, firstRun.length());
    }

    @Test
    void playStartsTheChosenPlaylistEvenWithTracksQueued() {
        player.playNext("q");

        assertEquals("a", player.play(abcd).getId(), "pressing play on a playlist starts that playlist");
        assertEquals("qbcd", ids(player.upNext(10)), "the queued track plays right after");
    }

    @Test
    void nullStrategyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> player.setShuffleStrategy(null));
        assertInstanceOf(InOrderStrategy.class, player.getShuffleStrategy());
    }

    // ______________ with the real dataset ______________

    @Test
    void worksWithTheRealDatasetIfPresent() throws Exception {
        Path real = Path.of("data", "spotify_tracks.csv");
        assumeTrue(Files.exists(real), "data/spotify_tracks.csv not downloaded; skipping");
        InMemoryTrackRepository library = new InMemoryTrackRepository(new SpotifyCsvLoader().load(real));
        PlaylistService realPlaylists = new PlaylistService(library);
        PlayerService realPlayer = new PlayerService(realPlaylists, library);
        Playlist gym = realPlaylists.createTempoPlaylist("Gym", 120, 130, 20);

        realPlayer.setShuffleStrategy(new WeightedByPopularityShuffle(new Random(42)));
        realPlayer.play(gym.getId());
        realPlayer.playNext("5SuOikwiRyPMVoIQDJUgSV");   // "Comedy"

        assertEquals("Comedy", realPlayer.next().orElseThrow().getTitle());
        assertEquals(19, realPlayer.upNext(100).size());   // 20 in the playlist, 1 played; Comedy came from the queue
    }

    // ______________ helpers ______________

    private String playlistOf(String... trackIds) {
        String id = playlists.createPlaylist("Test").getId();
        for (String trackId : trackIds) {
            playlists.addTrack(id, trackId);
        }
        return id;
    }

    private void skip(int times) {
        for (int i = 0; i < times; i++) {
            player.next();
        }
    }

    private String nowPlaying() {
        return player.nowPlaying().map(MediaItem::getId).orElse("-");
    }

    private static String id(Optional<MediaItem> item) {
        return item.map(MediaItem::getId).orElse("-");
    }

    private static String ids(List<MediaItem> items) {
        return String.join("", items.stream().map(MediaItem::getId).toList());
    }

    /** A simple strategy written as a lambda: plays the playlist backwards. */
    private static ShuffleStrategy reversed() {
        return items -> {
            List<MediaItem> copy = new ArrayList<>(items);
            java.util.Collections.reverse(copy);
            return copy;
        };
    }

    private static Track track(String id, int popularity) {
        return new Track.Builder()
                .id(id)
                .title(id)
                .artists("Artist")
                .album("Album")
                .genre("pop")
                .popularity(popularity)
                .duration(Duration.ofSeconds(60))
                .build();
    }
}
