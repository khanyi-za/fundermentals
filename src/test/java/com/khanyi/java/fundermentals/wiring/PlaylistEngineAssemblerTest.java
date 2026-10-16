package com.khanyi.java.fundermentals.wiring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.SmartPlaylist;
import com.khanyi.java.fundermentals.model.Track;
import com.khanyi.java.fundermentals.service.InOrderStrategy;
import com.khanyi.java.fundermentals.service.PlayerService;
import com.khanyi.java.fundermentals.service.PlaylistService;
import com.khanyi.java.fundermentals.service.RandomShuffle;
import com.khanyi.java.fundermentals.service.RepeatMode;
import com.khanyi.java.fundermentals.service.ShuffleStrategy;
import com.khanyi.java.fundermentals.service.WeightedByPopularityShuffle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PlaylistEngineAssemblerTest {

    private final List<Track> library = List.of(
            track("a", 10), track("b", 20), track("c", 30), track("d", 40), track("e", 50));

    private PlaylistEngineAssembler engine;

    // Captures the INFO "engine ready" log line
    private final Logger assemblerLog = (Logger) LoggerFactory.getLogger(PlaylistEngineAssembler.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        previousLevel = assemblerLog.getLevel();
        assemblerLog.setLevel(Level.INFO);
        logs.start();
        assemblerLog.addAppender(logs);

        engine = new PlaylistEngineAssembler(library, new Random(42));
    }

    @AfterEach
    void tearDown() {
        assemblerLog.detachAppender(logs);
        assemblerLog.setLevel(previousLevel);
    }

    // ______________ constructing ______________

    @Test
    void rejectsNullArguments() {
        Random random = new Random();

        assertThrows(IllegalArgumentException.class, () -> new PlaylistEngineAssembler(null, random));
        assertThrows(IllegalArgumentException.class, () -> new PlaylistEngineAssembler(library, null));
    }

    @Test
    void loadsTheLibraryIntoTheRepository() {
        assertEquals(5, engine.trackRepository().count());
        assertTrue(engine.trackRepository().findById("c").isPresent());
    }

    @Test
    void anEmptyLibraryStillGivesAWorkingEngine() {
        PlaylistEngineAssembler empty = new PlaylistEngineAssembler(List.of(), new Random());

        assertEquals(0, empty.trackRepository().count());
        assertTrue(empty.playlistService().allPlaylists().isEmpty());
        assertEquals(Optional.empty(), empty.playerService().nowPlaying());
    }

    @Test
    void logsThatTheEngineIsReady() {
        List<String> info = logs.list.stream()
                .filter(e -> e.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertEquals(List.of("Playlist engine ready: 5 tracks, shuffle strategies [in-order, random, weighted]"), info);
    }

    // ______________ one instance of each service ______________

    @Test
    void returnsTheSameServicesEveryTime() {
        assertSame(engine.trackRepository(), engine.trackRepository());
        assertSame(engine.playlistService(), engine.playlistService());
        assertSame(engine.playerService(), engine.playerService());
    }

    @Test
    void twoAssemblersBuildTwoSeparateEngines() {
        PlaylistEngineAssembler other = new PlaylistEngineAssembler(library, new Random(42));
        engine.playlistService().createPlaylist("Only in the first engine");

        assertNotSame(engine.playlistService(), other.playlistService());
        assertTrue(other.playlistService().allPlaylists().isEmpty());
    }

    @Test
    void servicesShareTheOneRepository() {
        // a track saved to the repository is visible to both services, so they were given the same one
        engine.trackRepository().save(track("new", 99));
        Playlist mix = engine.playlistService().createPlaylist("Mix");

        engine.playlistService().addTrack(mix.getId(), "new");
        engine.playerService().play(mix.getId());
        engine.playerService().playNext("new");

        assertEquals("new", engine.playerService().nowPlaying().orElseThrow().getId());
        assertEquals(1, engine.playerService().queueSize());
    }

    @Test
    void playerSeesPlaylistsMadeThroughThePlaylistService() {
        Playlist mix = engine.playlistService().createPlaylist("Mix");
        engine.playlistService().addTrack(mix.getId(), "b");
        engine.playlistService().addTrack(mix.getId(), "a");

        MediaItem first = engine.playerService().play(mix.getId());

        assertEquals("b", first.getId());
        assertSame(mix, engine.playerService().currentPlaylist().orElseThrow());
    }

    // ______________ shuffle strategies ______________

    @Test
    void offersTheThreeStrategiesInOrder() {
        assertEquals(List.of(IN_ORDER, RANDOM, WEIGHTED), new ArrayList<>(engine.shuffleStrategyNames()));
        assertEquals(List.of("in-order", "random", "weighted"), new ArrayList<>(engine.shuffleStrategyNames()));
    }

    @Test
    void looksUpEachStrategyByName() {
        assertInstanceOf(InOrderStrategy.class, engine.shuffleStrategy(IN_ORDER).orElseThrow());
        assertInstanceOf(RandomShuffle.class, engine.shuffleStrategy(RANDOM).orElseThrow());
        assertInstanceOf(WeightedByPopularityShuffle.class, engine.shuffleStrategy(WEIGHTED).orElseThrow());
    }

    @Test
    void unknownStrategyNameGivesEmpty() {
        assertEquals(Optional.empty(), engine.shuffleStrategy("alphabetical"));
        assertEquals(Optional.empty(), engine.shuffleStrategy("RANDOM"));   // names are case-sensitive
        assertEquals(Optional.empty(), engine.shuffleStrategy(""));
        assertEquals(Optional.empty(), engine.shuffleStrategy(null));
    }

    @Test
    void lookingUpTwiceGivesTheSameStrategyObject() {
        assertSame(engine.shuffleStrategy(RANDOM).orElseThrow(), engine.shuffleStrategy(RANDOM).orElseThrow());
    }

    @Test
    void strategyNamesCannotBeChangedFromOutside() {
        assertThrows(UnsupportedOperationException.class, () -> engine.shuffleStrategyNames().remove(RANDOM));
        assertThrows(UnsupportedOperationException.class, () -> engine.shuffleStrategyNames().clear());
        assertEquals(3, engine.shuffleStrategyNames().size());
    }

    @Test
    void playerStartsWithTheInOrderStrategy() {
        // setter injection: the assembler handed the player its starting strategy
        assertSame(engine.shuffleStrategy(IN_ORDER).orElseThrow(), engine.playerService().getShuffleStrategy());
        assertEquals(RepeatMode.OFF, engine.playerService().getRepeatMode());
    }

    @Test
    void bothRandomStrategiesUseTheOneInjectedRandom() {
        // build the same thing by hand: ONE Random(42) shared by both strategies
        Random shared = new Random(42);
        ShuffleStrategy randomByHand = new RandomShuffle(shared);
        ShuffleStrategy weightedByHand = new WeightedByPopularityShuffle(shared);
        List<MediaItem> items = new ArrayList<>(library);

        // the same calls, in the same order, must draw the same numbers from the same sequence
        assertEquals(randomByHand.order(items), engine.shuffleStrategy(RANDOM).orElseThrow().order(items));
        assertEquals(weightedByHand.order(items), engine.shuffleStrategy(WEIGHTED).orElseThrow().order(items));
        assertEquals(randomByHand.order(items), engine.shuffleStrategy(RANDOM).orElseThrow().order(items));
    }

    @Test
    void theSameSeedGivesTheSameShuffledPlayback() {
        assertEquals(shuffledPlayback(new Random(7)), shuffledPlayback(new Random(7)));
    }

    // ______________ end to end ______________

    @Test
    void wholeEngineWorksTogether() {
        PlaylistService playlists = engine.playlistService();
        PlayerService player = engine.playerService();
        Playlist mix = playlists.createPlaylist("Mix");
        for (String id : List.of("a", "b", "c")) {
            playlists.addTrack(mix.getId(), id);
        }
        playlists.moveItem(mix.getId(), 2, 0);   // c, a, b
        playlists.undo(mix.getId());             // a, b, c

        player.setShuffleStrategy(engine.shuffleStrategy(IN_ORDER).orElseThrow());
        player.setRepeatMode(RepeatMode.ALL);

        assertEquals("a", player.play(mix.getId()).getId());
        assertEquals("b", player.next().orElseThrow().getId());
        assertEquals("c", player.next().orElseThrow().getId());
        assertEquals("a", player.next().orElseThrow().getId(), "repeat ALL wraps round");
        assertEquals(List.of("c", "b", "a"), ids(player.recentlyPlayed()));
    }

    @Test
    void smartPlaylistsAreBuiltFromTheAssembledRepository() {
        SmartPlaylist pop = engine.playlistService().createGenrePlaylist("Pop", "pop", 3);

        // filled most popular first from the repository the assembler created
        assertEquals(List.of("e", "d", "c"), ids(pop.getItems()));
    }

    // ______________ fromSpotifyCsv ______________

    @Test
    void fromSpotifyCsvLoadsTheSampleFile() throws Exception {
        Path sample = Path.of(getClass().getResource("/data/spotify_sample.csv").toURI());

        PlaylistEngineAssembler fromCsv = PlaylistEngineAssembler.fromSpotifyCsv(sample);

        // 7 rows: 1 duplicate id and 1 blank row are skipped
        assertEquals(5, fromCsv.trackRepository().count());
        assertEquals("Too Good", fromCsv.trackRepository().getById("t3").getTitle());
        assertEquals(3, fromCsv.shuffleStrategyNames().size());
    }

    @Test
    void fromSpotifyCsvPassesOnAMissingFile(@TempDir Path dir) {
        Path missing = dir.resolve("no-such-file.csv");

        assertThrows(IOException.class, () -> PlaylistEngineAssembler.fromSpotifyCsv(missing));
    }

    @Test
    void worksWithTheRealDatasetIfPresent() throws Exception {
        Path real = Path.of("data", "spotify_tracks.csv");
        assumeTrue(Files.exists(real), "data/spotify_tracks.csv not downloaded; skipping");

        PlaylistEngineAssembler realEngine = PlaylistEngineAssembler.fromSpotifyCsv(real);
        SmartPlaylist jazz = realEngine.playlistService().createGenrePlaylist("Jazz", "jazz", 25);
        PlayerService player = realEngine.playerService();
        player.setShuffleStrategy(realEngine.shuffleStrategy(WEIGHTED).orElseThrow());

        assertEquals(89_740, realEngine.trackRepository().count());
        assertEquals(25, jazz.size());
        assertTrue(jazz.getItems().contains(player.play(jazz.getId())));
        assertEquals(24, player.upNext(30).size());
    }

    // ______________ helpers ______________

    /** A fresh engine with this Random: shuffle a 5-track playlist and play it right through. */
    private List<String> shuffledPlayback(Random random) {
        PlaylistEngineAssembler e = new PlaylistEngineAssembler(library, random);
        Playlist all = e.playlistService().createPlaylist("All");
        for (Track t : library) {
            e.playlistService().addTrack(all.getId(), t.getId());
        }
        PlayerService player = e.playerService();
        player.setShuffleStrategy(e.shuffleStrategy(RANDOM).orElseThrow());

        List<String> played = new ArrayList<>();
        played.add(player.play(all.getId()).getId());
        Optional<MediaItem> next = player.next();
        while (next.isPresent()) {
            played.add(next.get().getId());
            next = player.next();
        }
        return played;
    }

    private static List<String> ids(List<? extends MediaItem> items) {
        return items.stream().map(MediaItem::getId).toList();
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
