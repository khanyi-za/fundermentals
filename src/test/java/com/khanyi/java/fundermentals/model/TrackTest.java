package com.khanyi.java.fundermentals.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TrackTest {

    /** A fresh, valid builder; each test changes only what it is about. */
    private static Track.Builder validBuilder() {
        return new Track.Builder()
                .id("t1")
                .title("Comedy")
                .artists("Gen Hoshino")
                .album("Stranger")
                .genre("acoustic")
                .duration(Duration.ofMillis(230_666));
    }

    // ______________ building ______________

    @Test
    void buildStoresEveryField() {
        Track t = validBuilder()
                .popularity(73)
                .explicit(true)
                .tempo(87.917)
                .energy(0.461)
                .danceability(0.676)
                .valence(0.715)
                .build();

        assertEquals("t1", t.getId());
        assertEquals("Comedy", t.getTitle());
        assertEquals(List.of("Gen Hoshino"), t.getArtists());
        assertEquals("Stranger", t.getAlbum());
        assertEquals("acoustic", t.getGenre());
        assertEquals(Duration.ofMillis(230_666), t.getDuration());
        assertEquals(73, t.getPopularity());
        assertTrue(t.isExplicit());
        assertEquals(87.917, t.getTempo());
        assertEquals(0.461, t.getEnergy());
        assertEquals(0.676, t.getDanceability());
        assertEquals(0.715, t.getValence());
    }

    @Test
    void optionalNumbersDefaultToZeroAndExplicitToFalse() {
        Track t = validBuilder().build();

        assertEquals(0, t.getPopularity());
        assertFalse(t.isExplicit());
        assertEquals(0.0, t.getTempo());
        assertEquals(0.0, t.getEnergy());
        assertEquals(0.0, t.getDanceability());
        assertEquals(0.0, t.getValence());
    }

    @Test
    void textFieldsAreStripped() {
        Track t = validBuilder()
                .id("  t1 ")
                .title(" Comedy ")
                .artists(" Gen Hoshino ")
                .album(" Stranger ")
                .genre(" acoustic ")
                .build();

        assertEquals("t1", t.getId());
        assertEquals("Comedy", t.getTitle());
        assertEquals(List.of("Gen Hoshino"), t.getArtists());
        assertEquals("Stranger", t.getAlbum());
        assertEquals("acoustic", t.getGenre());
    }

    // ______________ artists ______________

    @Test
    void multipleArtistsAreJoinedIntoTheCreator() {
        Track t = validBuilder().artists("Drake", "Rihanna").build();

        assertEquals(List.of("Drake", "Rihanna"), t.getArtists());
        assertEquals("Drake, Rihanna", t.getCreator());
    }

    @Test
    void artistsListCannotBeModified() {
        Track t = validBuilder().build();

        assertThrows(UnsupportedOperationException.class, () -> t.getArtists().add("Someone"));
    }

    @Test
    void changingTheCallersArrayAfterBuildDoesNotChangeTheTrack() {
        String[] names = {"Drake", "Rihanna"};
        Track t = validBuilder().artists(names).build();

        names[0] = "Changed";

        assertEquals(List.of("Drake", "Rihanna"), t.getArtists());
    }

    @Test
    void aTrackNeedsAtLeastOneArtist() {
        assertThrows(IllegalArgumentException.class, () -> validBuilder().artists().build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().artists((String[]) null).build());
    }

    @Test
    void blankOrNullArtistIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> validBuilder().artists("Drake", " ").build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().artists("Drake", null).build());
    }

    // ______________ required fields ______________

    @Test
    void missingRequiredTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> validBuilder().id(null).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().id(" ").build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().title(null).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().title("").build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().album(null).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().genre(" ").build());
    }

    @Test
    void errorMessageNamesTheBadField() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> validBuilder().album(" ").build());

        assertEquals("album must not be blank", ex.getMessage());
    }

    @Test
    void durationMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> validBuilder().duration(null).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().duration(Duration.ZERO).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().duration(Duration.ofSeconds(-1)).build());
    }

    // ______________ number ranges ______________

    @Test
    void popularityMustBeBetween0And100() {
        assertEquals(0, validBuilder().popularity(0).build().getPopularity());
        assertEquals(100, validBuilder().popularity(100).build().getPopularity());

        assertThrows(IllegalArgumentException.class, () -> validBuilder().popularity(-1).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().popularity(101).build());
    }

    @Test
    void tempoMustBeAFiniteNumberZeroOrMore() {
        assertEquals(0.0, validBuilder().tempo(0.0).build().getTempo());

        assertThrows(IllegalArgumentException.class, () -> validBuilder().tempo(-0.1).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().tempo(Double.NaN).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().tempo(Double.POSITIVE_INFINITY).build());
    }

    @Test
    void audioFeaturesMustBeBetween0And1() {
        assertEquals(0.0, validBuilder().energy(0.0).build().getEnergy());
        assertEquals(1.0, validBuilder().energy(1.0).build().getEnergy());

        assertThrows(IllegalArgumentException.class, () -> validBuilder().energy(-0.01).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().energy(1.01).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().danceability(2.0).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().valence(-1.0).build());
    }

    @Test
    void nanIsRejectedForAudioFeatures() {
        assertThrows(IllegalArgumentException.class, () -> validBuilder().energy(Double.NaN).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().danceability(Double.NaN).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().valence(Double.NaN).build());
    }

    // ______________ display ______________

    @Test
    void displayNameIsCreatorDashTitle() {
        assertEquals("Gen Hoshino - Comedy", validBuilder().build().getDisplayName());
    }

    @Test
    void formattedDurationUsesMinutesAndSeconds() {
        assertEquals("3:50", validBuilder().build().formattedDuration());
        assertEquals("0:59", validBuilder().duration(Duration.ofSeconds(59)).build().formattedDuration());
    }

    @Test
    void formattedDurationAddsHoursWhenAnHourOrLonger() {
        Track t = validBuilder().duration(Duration.ofHours(1).plusMinutes(2).plusSeconds(3)).build();

        assertEquals("1:02:03", t.formattedDuration());
    }

    @Test
    void toStringShowsTypeIdDisplayNameAndDuration() {
        assertEquals("Track[t1: Gen Hoshino - Comedy (3:50)]", validBuilder().build().toString());
    }

    // ______________ search ______________

    @Test
    void matchesTitleArtistAlbumAndGenre() {
        Track t = validBuilder().build();

        assertTrue(t.matches("comedy"));
        assertTrue(t.matches("hoshino"));
        assertTrue(t.matches("stranger"));
        assertTrue(t.matches("acoustic"));
    }

    @Test
    void searchIsCaseInsensitiveAndIgnoresSurroundingSpaces() {
        Track t = validBuilder().build();

        assertTrue(t.matches("GEN HOSHINO"));
        assertTrue(t.matches("  comedy  "));
    }

    @Test
    void searchMatchesPartOfAWord() {
        assertTrue(validBuilder().build().matches("hosh"));
    }

    @Test
    void blankOrNullQueryMatchesEverything() {
        Track t = validBuilder().build();

        assertTrue(t.matches(""));
        assertTrue(t.matches("   "));
        assertTrue(t.matches(null));
    }

    @Test
    void unrelatedQueryDoesNotMatch() {
        assertFalse(validBuilder().build().matches("metallica"));
    }

    @Test
    void searchableFieldsAreTitleArtistsAlbumGenre() {
        Track t = validBuilder().artists("Drake", "Rihanna").build();

        assertEquals(List.of("Comedy", "Drake, Rihanna", "Stranger", "acoustic"), t.searchableFields());
    }

    // ______________ equality ______________

    @Test
    void tracksWithTheSameIdAreEqualEvenIfOtherFieldsDiffer() {
        Track a = validBuilder().build();
        Track b = validBuilder().title("Another title").popularity(5).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void tracksWithDifferentIdsAreNotEqual() {
        assertNotEquals(validBuilder().build(), validBuilder().id("t2").build());
    }

    @Test
    void aTrackIsNotEqualToNullOrAnotherType() {
        Track t = validBuilder().build();

        assertNotEquals(null, t);
        assertNotEquals("t1", t);
    }
}
