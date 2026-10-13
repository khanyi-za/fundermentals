package com.khanyi.java.fundermentals.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rules shared by every MediaItem, checked through both subclasses
 * and through the Playable and Searchable interface types.
 */
public class MediaItemTest {

    private static Track track(String id, String title) {
        return new Track.Builder()
                .id(id)
                .title(title)
                .artists("Gen Hoshino")
                .album("Stranger")
                .genre("acoustic")
                .duration(Duration.ofSeconds(225))
                .build();
    }

    private static PodcastEpisode episode(String id, String title) {
        return new PodcastEpisode(id, title, Duration.ofMinutes(45), "Java Talk", 3, "Ada Lovelace");
    }

    // ______________ the class design ______________

    @Test
    void mediaItemIsAbstract() {
        assertTrue(Modifier.isAbstract(MediaItem.class.getModifiers()));
    }

    @Test
    void equalsAndHashCodeCannotBeOverridden() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(MediaItem.class.getMethod("equals", Object.class).getModifiers()));
        assertTrue(Modifier.isFinal(MediaItem.class.getMethod("hashCode").getModifiers()));
    }

    @Test
    void subclassesCannotBeExtendedFurther() {
        assertTrue(Modifier.isFinal(Track.class.getModifiers()));
        assertTrue(Modifier.isFinal(PodcastEpisode.class.getModifiers()));
    }

    // ______________ equality across types ______________

    @Test
    void aTrackAndAnEpisodeWithTheSameIdAreNotEqual() {
        MediaItem t = track("same-id", "Title");
        MediaItem e = episode("same-id", "Title");

        assertNotEquals(t, e);
        assertNotEquals(e, t);
    }

    @Test
    void aHashSetKeepsOneCopyOfEachId() {
        Set<MediaItem> set = new HashSet<>();
        set.add(track("t1", "Comedy"));
        set.add(track("t1", "Comedy (Live)"));   // same id: a duplicate
        set.add(track("t2", "Other"));
        set.add(episode("t1", "Episode"));       // same id, different type: not a duplicate

        assertEquals(3, set.size());
    }

    @Test
    void listContainsFindsAnEqualCopy() {
        List<MediaItem> playlist = new ArrayList<>(List.of(track("t1", "Comedy")));

        assertTrue(playlist.contains(track("t1", "Comedy")));
    }

    // ______________ polymorphism ______________

    @Test
    void eachSubclassSuppliesItsOwnCreator() {
        List<MediaItem> items = List.of(track("t1", "Comedy"), episode("e1", "Inside the JVM"));

        assertEquals("Gen Hoshino", items.get(0).getCreator());
        assertEquals("Java Talk", items.get(1).getCreator());
    }

    @Test
    void displayNameIsInheritedByTrackAndOverriddenByEpisode() {
        List<MediaItem> items = List.of(track("t1", "Comedy"), episode("e1", "Inside the JVM"));

        assertEquals("Gen Hoshino - Comedy", items.get(0).getDisplayName());
        assertEquals("Java Talk #3 - Inside the JVM", items.get(1).getDisplayName());
    }

    @Test
    void toStringNamesTheRealType() {
        assertTrue(track("t1", "Comedy").toString().startsWith("Track["));
        assertTrue(episode("e1", "Talk").toString().startsWith("PodcastEpisode["));
    }

    // ______________ through the interfaces ______________

    @Test
    void bothKindsWorkAsPlayable() {
        List<Playable> queue = List.of(track("t1", "Comedy"), episode("e1", "Inside the JVM"));

        Duration total = Duration.ZERO;
        for (Playable p : queue) {
            total = total.plus(p.getDuration());
        }

        assertEquals(Duration.ofSeconds(225).plusMinutes(45), total);
        assertEquals("3:45", queue.get(0).formattedDuration());
        assertEquals("45:00", queue.get(1).formattedDuration());
    }

    @Test
    void bothKindsWorkAsSearchable() {
        List<Searchable> library = List.of(track("t1", "Comedy"), episode("e1", "Inside the JVM"));

        List<Searchable> hits = new ArrayList<>();
        for (Searchable s : library) {
            if (s.matches("java")) {
                hits.add(s);
            }
        }

        assertEquals(1, hits.size());
        assertInstanceOf(PodcastEpisode.class, hits.get(0));
    }

    @Test
    void formattedDurationAtExactlyOneHour() {
        Playable p = new PodcastEpisode("e1", "Title", Duration.ofHours(1), "Show", 1);

        assertEquals("1:00:00", p.formattedDuration());
    }

    @Test
    void containsIgnoreCaseIsNullSafe() {
        assertFalse(Searchable.containsIgnoreCase(null, "x"));
        assertTrue(Searchable.containsIgnoreCase("Hello World", "world"));
    }
}
