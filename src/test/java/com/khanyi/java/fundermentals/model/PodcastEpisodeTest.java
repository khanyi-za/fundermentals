package com.khanyi.java.fundermentals.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PodcastEpisodeTest {

    private static final Duration ONE_HOUR_TWO_MIN_THREE_SEC =
            Duration.ofHours(1).plusMinutes(2).plusSeconds(3);

    private static PodcastEpisode episode() {
        return new PodcastEpisode("e1", "Inside the JVM", ONE_HOUR_TWO_MIN_THREE_SEC,
                "Java Talk", 12, "Ada Lovelace");
    }

    // ______________ constructing ______________

    @Test
    void constructorStoresEveryField() {
        PodcastEpisode e = episode();

        assertEquals("e1", e.getId());
        assertEquals("Inside the JVM", e.getTitle());
        assertEquals(ONE_HOUR_TWO_MIN_THREE_SEC, e.getDuration());
        assertEquals("Java Talk", e.getShow());
        assertEquals(12, e.getEpisodeNumber());
        assertEquals(Optional.of("Ada Lovelace"), e.getHost());
    }

    @Test
    void textFieldsAreStripped() {
        PodcastEpisode e = new PodcastEpisode(" e1 ", " Title ", Duration.ofMinutes(30),
                " Java Talk ", 1, " Ada Lovelace ");

        assertEquals("e1", e.getId());
        assertEquals("Title", e.getTitle());
        assertEquals("Java Talk", e.getShow());
        assertEquals(Optional.of("Ada Lovelace"), e.getHost());
    }

    @Test
    void shorterConstructorLeavesHostEmpty() {
        PodcastEpisode e = new PodcastEpisode("e1", "Title", Duration.ofMinutes(30), "Java Talk", 1);

        assertEquals(Optional.empty(), e.getHost());
    }

    @Test
    void blankOrNullHostBecomesEmpty() {
        PodcastEpisode blank = new PodcastEpisode("e1", "Title", Duration.ofMinutes(30), "Java Talk", 1, "  ");
        PodcastEpisode none = new PodcastEpisode("e2", "Title", Duration.ofMinutes(30), "Java Talk", 1, null);

        assertTrue(blank.getHost().isEmpty());
        assertTrue(none.getHost().isEmpty());
    }

    @Test
    void episodeNumberMustBeAtLeastOne() {
        assertEquals(1, new PodcastEpisode("e1", "Title", Duration.ofMinutes(30), "Show", 1).getEpisodeNumber());

        assertThrows(IllegalArgumentException.class,
                () -> new PodcastEpisode("e1", "Title", Duration.ofMinutes(30), "Show", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PodcastEpisode("e1", "Title", Duration.ofMinutes(30), "Show", -3));
    }

    @Test
    void showIsRequired() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PodcastEpisode("e1", "Title", Duration.ofMinutes(30), " ", 1));

        assertEquals("show must not be blank", ex.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> new PodcastEpisode("e1", "Title", Duration.ofMinutes(30), null, 1));
    }

    @Test
    void sharedMediaItemRulesStillApply() {
        assertThrows(IllegalArgumentException.class,
                () -> new PodcastEpisode(null, "Title", Duration.ofMinutes(30), "Show", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new PodcastEpisode("e1", " ", Duration.ofMinutes(30), "Show", 1));
        assertThrows(IllegalArgumentException.class,
                () -> new PodcastEpisode("e1", "Title", Duration.ZERO, "Show", 1));
    }

    // ______________ display ______________

    @Test
    void creatorIsTheShow() {
        assertEquals("Java Talk", episode().getCreator());
    }

    @Test
    void displayNameIncludesTheEpisodeNumber() {
        assertEquals("Java Talk #12 - Inside the JVM", episode().getDisplayName());
    }

    @Test
    void formattedDurationIncludesHours() {
        assertEquals("1:02:03", episode().formattedDuration());
    }

    @Test
    void toStringUsesTheOverriddenDisplayName() {
        assertEquals("PodcastEpisode[e1: Java Talk #12 - Inside the JVM (1:02:03)]", episode().toString());
    }

    // ______________ search ______________

    @Test
    void matchesTitleShowAndHost() {
        PodcastEpisode e = episode();

        assertTrue(e.matches("jvm"));
        assertTrue(e.matches("java talk"));
        assertTrue(e.matches("lovelace"));
        assertFalse(e.matches("python"));
    }

    @Test
    void searchWithoutAHostDoesNotThrow() {
        PodcastEpisode e = new PodcastEpisode("e1", "Title", Duration.ofMinutes(30), "Java Talk", 1);

        assertFalse(e.matches("lovelace"));
        assertTrue(e.matches("java"));
    }

    @Test
    void searchableFieldsAreTitleShowHost() {
        assertEquals(List.of("Inside the JVM", "Java Talk", "Ada Lovelace"), episode().searchableFields());
    }

    // ______________ equality ______________

    @Test
    void episodesWithTheSameIdAreEqual() {
        PodcastEpisode a = episode();
        PodcastEpisode b = new PodcastEpisode("e1", "Other", Duration.ofMinutes(5), "Other Show", 99);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void episodesWithDifferentIdsAreNotEqual() {
        PodcastEpisode other = new PodcastEpisode("e2", "Inside the JVM", ONE_HOUR_TWO_MIN_THREE_SEC,
                "Java Talk", 12, "Ada Lovelace");

        assertNotEquals(episode(), other);
    }
}
