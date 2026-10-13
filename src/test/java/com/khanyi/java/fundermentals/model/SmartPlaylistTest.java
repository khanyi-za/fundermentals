package com.khanyi.java.fundermentals.model;

import com.khanyi.java.fundermentals.data.InMemoryTrackRepository;
import com.khanyi.java.fundermentals.data.SpotifyCsvLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SmartPlaylistTest {

    private Track rockSlow;
    private Track rockFast;
    private Track popFast;
    private Track jazzMid;
    private Track rockMid;
    private List<Track> library;

    @BeforeEach
    void setUp() {
        rockSlow = track("r1", "rock", 90, 50);
        rockFast = track("r2", "rock", 140, 70);
        popFast = track("p1", "pop", 125, 90);
        jazzMid = track("j1", "jazz", 120, 30);
        rockMid = track("r3", "Rock", 128, 60);
        library = List.of(rockSlow, rockFast, popFast, jazzMid, rockMid);
    }

    // ______________ constructing ______________

    @Test
    void constructorKeepsTheRuleDetails() {
        SmartPlaylist p = new SmartPlaylist("s1", "Long", "  over 2 minutes ",
                t -> t.getDuration().toSeconds() > 120, 10);

        assertEquals("s1", p.getId());
        assertEquals("Long", p.getName());
        assertEquals("over 2 minutes", p.getRuleDescription());
        assertEquals(10, p.getLimit());
        assertTrue(p.isEmpty(), "a smart playlist starts empty until refreshed");
    }

    @Test
    void constructorRejectsBadArguments() {
        Predicate<Track> any = t -> true;

        assertThrows(IllegalArgumentException.class, () -> new SmartPlaylist("s", "n", " ", any, 1));
        assertThrows(IllegalArgumentException.class, () -> new SmartPlaylist("s", "n", "d", null, 1));
        assertThrows(IllegalArgumentException.class, () -> new SmartPlaylist("s", "n", "d", any, 0));
        assertThrows(IllegalArgumentException.class, () -> new SmartPlaylist("", "n", "d", any, 1),
                "the parent's checks still run");
    }

    @Test
    void isAPlaylist() {
        Playlist p = SmartPlaylist.byGenre("s1", "Rock", "rock", 5);

        assertInstanceOf(Playlist.class, p);
        assertTrue(p.toString().startsWith("SmartPlaylist[s1: Rock, 0 items]"));
    }

    // ______________ byGenre ______________

    @Test
    void byGenreMatchesIgnoringCaseAndSpaces() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "  ROCK ", 10);

        assertEquals("genre is rock", rock.getRuleDescription());
        assertTrue(rock.matches(rockSlow));
        assertTrue(rock.matches(rockMid));   // genre "Rock"
        assertFalse(rock.matches(popFast));
    }

    @Test
    void byGenreIsExactNotContains() {
        Track hardRock = track("h1", "hard-rock", 120, 50);

        assertFalse(SmartPlaylist.byGenre("s1", "Rock", "rock", 10).matches(hardRock));
    }

    @Test
    void byGenreRejectsBlankGenre() {
        assertThrows(IllegalArgumentException.class, () -> SmartPlaylist.byGenre("s1", "x", " ", 10));
        assertThrows(IllegalArgumentException.class, () -> SmartPlaylist.byGenre("s1", "x", null, 10));
    }

    // ______________ byTempo ______________

    @Test
    void byTempoIncludesBothEnds() {
        SmartPlaylist gym = SmartPlaylist.byTempo("s2", "Gym", 120, 128, 10);

        assertEquals("tempo 120.0 to 128.0 BPM", gym.getRuleDescription());
        assertTrue(gym.matches(jazzMid));    // 120
        assertTrue(gym.matches(popFast));    // 125
        assertTrue(gym.matches(rockMid));    // 128
        assertFalse(gym.matches(rockSlow));  // 90
        assertFalse(gym.matches(rockFast));  // 140
    }

    @Test
    void byTempoRejectsBadRanges() {
        assertThrows(IllegalArgumentException.class, () -> SmartPlaylist.byTempo("s", "n", 130, 120, 5));
        assertThrows(IllegalArgumentException.class, () -> SmartPlaylist.byTempo("s", "n", -1, 120, 5));
        assertThrows(IllegalArgumentException.class, () -> SmartPlaylist.byTempo("s", "n", Double.NaN, 120, 5));
        assertThrows(IllegalArgumentException.class, () -> SmartPlaylist.byTempo("s", "n", 100, Double.NaN, 5));
    }

    @Test
    void byTempoAllowsASingleValueRange() {
        assertTrue(SmartPlaylist.byTempo("s", "n", 120, 120, 5).matches(jazzMid));
    }

    // ______________ matches ______________

    @Test
    void podcastEpisodesNeverMatch() {
        PodcastEpisode episode = new PodcastEpisode("e1", "Pilot", Duration.ofMinutes(30), "Show", 1);
        SmartPlaylist anything = new SmartPlaylist("s", "n", "every track", t -> true, 5);

        assertFalse(anything.matches(episode));
        assertFalse(anything.matches(null));
    }

    @Test
    void ruleCanBeWrittenForAParentType() {
        // Predicate<MediaItem>, not Predicate<Track>: allowed by Predicate<? super Track>
        Predicate<MediaItem> overTwoMinutes = item -> item.getDuration().toSeconds() > 120;
        SmartPlaylist p = new SmartPlaylist("s", "Long", "over 2 minutes", overTwoMinutes, 5);

        p.refresh(List.of(track("short", "pop", 100, 50, 90), track("long", "pop", 100, 50, 200)));

        assertEquals(List.of("long"), ids(p));
    }

    @Test
    void rulesCanBeCombined() {
        Predicate<Track> rock = t -> t.getGenre().equalsIgnoreCase("rock");
        Predicate<Track> fast = t -> t.getTempo() > 120;
        SmartPlaylist p = new SmartPlaylist("s", "Fast rock", "fast rock", rock.and(fast), 10);

        p.refresh(library);

        assertEquals(List.of("r2", "r3"), ids(p));
    }

    // ______________ refresh ______________

    @Test
    void refreshFillsWithMatchesInLibraryOrder() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);

        assertEquals(3, rock.refresh(library));
        assertEquals(List.of("r1", "r2", "r3"), ids(rock));
    }

    @Test
    void refreshStopsAtTheLimit() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 2);

        assertEquals(2, rock.refresh(library));
        assertEquals(List.of("r1", "r2"), ids(rock));
    }

    @Test
    void theLibrarysOrderDecidesWhichMatchesWin() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 2);
        InMemoryTrackRepository repo = new InMemoryTrackRepository(library);

        rock.refresh(repo.sorted(InMemoryTrackRepository.MOST_POPULAR_FIRST));

        assertEquals(List.of("r2", "r3"), ids(rock));   // popularity 70 and 60 beat 50
    }

    @Test
    void refreshReplacesTheOldContents() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);
        rock.refresh(library);

        assertEquals(1, rock.refresh(List.of(rockFast, popFast)));
        assertEquals(List.of("r2"), ids(rock));
    }

    @Test
    void refreshFromAnEmptyLibraryEmptiesThePlaylist() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);
        rock.refresh(library);

        assertEquals(0, rock.refresh(List.of()));
        assertTrue(rock.isEmpty());
    }

    @Test
    void refreshWithNoMatchesIsEmpty() {
        SmartPlaylist metal = SmartPlaylist.byGenre("s1", "Metal", "metal", 10);

        assertEquals(0, metal.refresh(library));
    }

    @Test
    void refreshRejectsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> SmartPlaylist.byGenre("s1", "Rock", "rock", 10).refresh(null));
    }

    @Test
    void refreshIsLazyAndStopsReadingAtTheLimit() {
        int[] tested = {0};
        SmartPlaylist p = new SmartPlaylist("s", "Any", "anything",
                t -> { tested[0]++; return true; }, 2);

        p.refresh(library);

        // 2 tested by FilteringIterator, then 2 more by checkCanAdd when they are added
        assertEquals(4, tested[0], "the last three tracks must never be looked at");
    }

    // ______________ the hook: adding by hand ______________

    @Test
    void addingAMatchingTrackByHandWorks() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);

        rock.add(rockFast);
        rock.insert(0, rockSlow);

        assertEquals(List.of("r1", "r2"), ids(rock));
    }

    @Test
    void addingANonMatchingTrackIsRefused() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);
        rock.add(rockFast);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> rock.add(popFast));

        assertEquals("'Title p1' doesn't match this playlist's rule: genre is rock", ex.getMessage());
        assertThrows(IllegalArgumentException.class, () -> rock.insert(0, jazzMid));
        assertEquals(List.of("r2"), ids(rock));
    }

    @Test
    void addingAPodcastEpisodeIsRefused() {
        SmartPlaylist any = new SmartPlaylist("s", "n", "every track", t -> true, 5);
        PodcastEpisode episode = new PodcastEpisode("e1", "Pilot", Duration.ofMinutes(30), "Show", 1);

        assertThrows(IllegalArgumentException.class, () -> any.add(episode));
    }

    @Test
    void refusalWorksThroughAPlaylistReference() {
        // polymorphism: the caller only knows it's a Playlist, but the override still runs
        Playlist p = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);

        assertThrows(IllegalArgumentException.class, () -> p.add(popFast));
    }

    @Test
    void inheritedEditingStillWorks() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);
        rock.refresh(library);

        rock.move(0, 2);
        rock.removeAt(0);

        assertEquals(List.of("r3", "r1"), ids(rock));
        assertEquals(Duration.ofSeconds(240), rock.getTotalDuration());   // two tracks of 120 s
    }

    @Test
    void handAddedTracksMayGoPastTheLimit() {
        // the limit is for refresh; a person adding by hand is a deliberate choice
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 1);
        rock.refresh(library);

        rock.add(rockFast);

        assertEquals(2, rock.size());
    }

    @Test
    void toStringAddsTheRule() {
        SmartPlaylist rock = SmartPlaylist.byGenre("s1", "Rock", "rock", 10);
        rock.refresh(library);

        assertEquals("SmartPlaylist[s1: Rock, 3 items] where genre is rock", rock.toString());
    }

    // ______________ with the real dataset ______________

    @Test
    void worksWithTheRealDatasetIfPresent() throws Exception {
        Path real = Path.of("data", "spotify_tracks.csv");
        assumeTrue(Files.exists(real), "data/spotify_tracks.csv not downloaded; skipping");
        InMemoryTrackRepository repo = new InMemoryTrackRepository(new SpotifyCsvLoader().load(real));

        SmartPlaylist gym = SmartPlaylist.byTempo("s", "Gym", 120, 130, 50);
        gym.refresh(repo.sorted(InMemoryTrackRepository.MOST_POPULAR_FIRST));

        assertEquals(50, gym.size());
        for (MediaItem item : gym) {
            double tempo = ((Track) item).getTempo();
            assertTrue(tempo >= 120 && tempo <= 130);
        }
    }

    // ______________ helpers ______________

    private static List<String> ids(Playlist p) {
        return p.getItems().stream().map(MediaItem::getId).toList();
    }

    private static Track track(String id, String genre, double tempo, int popularity) {
        return track(id, genre, tempo, popularity, 120);
    }

    private static Track track(String id, String genre, double tempo, int popularity, int seconds) {
        return new Track.Builder()
                .id(id)
                .title("Title " + id)
                .artists("Artist")
                .album("Album")
                .genre(genre)
                .tempo(tempo)
                .popularity(popularity)
                .duration(Duration.ofSeconds(seconds))
                .build();
    }
}
