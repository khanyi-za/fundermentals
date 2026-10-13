package com.khanyi.java.fundermentals.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A song from the Spotify dataset.
 * It has too many fields for a readable constructor, so it is created with a Builder:
 *
 *   Track t = new Track.Builder()
 *           .id("5SuOikwiRyPMVoIQDJUgSV").title("Comedy").artists("Gen Hoshino")
 *           .album("Comedy").genre("acoustic").duration(Duration.ofMillis(230666))
 *           .popularity(73).tempo(87.917).energy(0.461)
 *           .build();
 */
public final class Track extends MediaItem {

    private final List<String> artists;
    private final String album;
    private final String genre;
    private final int popularity;       // 0-100, Spotify's own score
    private final boolean explicit;
    private final double tempo;         // beats per minute
    private final double energy;        // 0.0-1.0
    private final double danceability;  // 0.0-1.0
    private final double valence;       // 0.0-1.0, how "happy" it sounds

    /** private: the only way to make a Track is through the Builder */
    private Track(Builder b) {
        super(b.id, b.title, b.duration);
        this.artists = requireArtists(b.artists);
        this.album = requireText(b.album, "album");
        this.genre = requireText(b.genre, "genre");
        if (b.popularity < 0 || b.popularity > 100) {
            throw new IllegalArgumentException("popularity must be between 0 and 100");
        }
        this.popularity = b.popularity;
        this.explicit = b.explicit;
        if (!Double.isFinite(b.tempo) || b.tempo < 0) {
            throw new IllegalArgumentException("tempo must be zero or more");
        }
        this.tempo = b.tempo;
        this.energy = requireUnitRange(b.energy, "energy");
        this.danceability = requireUnitRange(b.danceability, "danceability");
        this.valence = requireUnitRange(b.valence, "valence");
    }

    // ______________ getters ______________

    /** Already unmodifiable (List.copyOf), so it's safe to hand out. */
    public List<String> getArtists() {
        return artists;
    }

    public String getAlbum() {
        return album;
    }

    public String getGenre() {
        return genre;
    }

    public int getPopularity() {
        return popularity;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public double getTempo() {
        return tempo;
    }

    public double getEnergy() {
        return energy;
    }

    public double getDanceability() {
        return danceability;
    }

    public double getValence() {
        return valence;
    }

    // ______________ MediaItem overrides ______________

    /** "Artist A, Artist B" */
    @Override
    public String getCreator() {
        return String.join(", ", artists);
    }

    /** Title and artists come from MediaItem; a track is also searchable by album and genre. */
    @Override
    public List<String> searchableFields() {
        List<String> fields = super.searchableFields();
        fields.add(album);
        fields.add(genre);
        return fields;
    }

    // ______________ private helpers ______________

    private static List<String> requireArtists(List<String> names) {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("a track needs at least one artist");
        }
        List<String> cleaned = new ArrayList<>();
        for (String name : names) {
            cleaned.add(requireText(name, "artist"));
        }
        return List.copyOf(cleaned);
    }

    /** Written as !(in range) so NaN is rejected too: every comparison with NaN is false. */
    private static double requireUnitRange(double value, String fieldName) {
        if (!(value >= 0.0 && value <= 1.0)) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
        }
        return value;
    }

    // ______________ builder ______________

    /**
     * Collects the values one named call at a time, then build() hands them all
     * to the private constructor, which checks them. Each setter returns this,
     * so calls can be chained.
     */
    public static final class Builder {
        private String id;
        private String title;
        private Duration duration;
        private List<String> artists = List.of();
        private String album;
        private String genre;
        private int popularity;
        private boolean explicit;
        private double tempo;
        private double energy;
        private double danceability;
        private double valence;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        /** Varargs: accepts artists("A"), artists("A", "B") or a String[] */
        public Builder artists(String... artists) {
            this.artists = artists == null ? null : Arrays.asList(artists);
            return this;
        }

        public Builder album(String album) {
            this.album = album;
            return this;
        }

        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder popularity(int popularity) {
            this.popularity = popularity;
            return this;
        }

        public Builder explicit(boolean explicit) {
            this.explicit = explicit;
            return this;
        }

        public Builder tempo(double tempo) {
            this.tempo = tempo;
            return this;
        }

        public Builder energy(double energy) {
            this.energy = energy;
            return this;
        }

        public Builder danceability(double danceability) {
            this.danceability = danceability;
            return this;
        }

        public Builder valence(double valence) {
            this.valence = valence;
            return this;
        }

        public Track build() {
            return new Track(this);
        }
    }
}
