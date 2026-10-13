package com.khanyi.java.fundermentals.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The shared base of everything a playlist can hold.
 * Abstract: there is no plain "media item", only a Track or a PodcastEpisode.
 * Immutable: every field is final and there are no setters.
 */
public abstract class MediaItem implements Playable, Searchable {

    private final String id;
    private final String title;
    private final Duration duration;

    /** protected: only subclasses can call it, through super(...) */
    protected MediaItem(String id, String title, Duration duration) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    /**
     * Who made it: a track's artists, an episode's show.
     * Abstract, so each subclass must decide.
     */
    public abstract String getCreator();

    /** Built from getCreator(), so it works for every subclass without being rewritten. */
    @Override
    public String getDisplayName() {
        return getCreator() + " - " + title;
    }

    /** Subclasses add their own fields by calling super.searchableFields() first. */
    @Override
    public List<String> searchableFields() {
        List<String> fields = new ArrayList<>();
        fields.add(title);
        fields.add(getCreator());
        return fields;
    }

    /**
     * Two items are equal when they are the same kind and have the same id.
     * final: subclasses can't change the rule and break equals/hashCode.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id.equals(((MediaItem) o).id);
    }

    /** Must agree with equals: equal objects must have equal hash codes. */
    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + ": " + getDisplayName() + " (" + formattedDuration() + ")]";
    }

    /** Shared by subclasses to validate their own text fields. */
    protected static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }
}
