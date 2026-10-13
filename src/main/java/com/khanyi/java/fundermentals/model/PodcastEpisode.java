package com.khanyi.java.fundermentals.model;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * One episode of a podcast show.
 * The Spotify dataset has no podcasts, so these are created by hand. They exist to show
 * polymorphism: a playlist of MediaItems can hold tracks and episodes side by side.
 *
 * Only a few fields, so a plain constructor reads fine here (compare Track's Builder).
 */
public final class PodcastEpisode extends MediaItem {

    private final String show;
    private final int episodeNumber;
    private final String host;          // optional: may be null

    public PodcastEpisode(String id, String title, Duration duration,
                          String show, int episodeNumber, String host) {
        super(id, title, duration);
        this.show = requireText(show, "show");
        if (episodeNumber < 1) {
            throw new IllegalArgumentException("episodeNumber must be 1 or more");
        }
        this.episodeNumber = episodeNumber;
        this.host = (host == null || host.isBlank()) ? null : host.strip();
    }

    /** Convenience constructor for episodes without a named host. */
    public PodcastEpisode(String id, String title, Duration duration, String show, int episodeNumber) {
        this(id, title, duration, show, episodeNumber, null);
    }

    // ______________ getters ______________

    public String getShow() {
        return show;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    /** Optional makes "there may be no host" part of the method's type, so callers can't forget it. */
    public Optional<String> getHost() {
        return Optional.ofNullable(host);
    }

    // ______________ MediaItem overrides ______________

    /** For an episode, the "creator" is the show. */
    @Override
    public String getCreator() {
        return show;
    }

    /** Overrides MediaItem's "Creator - Title" to add the episode number: "Show #12 - Title" */
    @Override
    public String getDisplayName() {
        return show + " #" + episodeNumber + " - " + getTitle();
    }

    /** Title and show come from MediaItem; an episode is also searchable by host. */
    @Override
    public List<String> searchableFields() {
        List<String> fields = super.searchableFields();
        fields.add(host);   // may be null; Searchable.containsIgnoreCase is null-safe
        return fields;
    }
}
