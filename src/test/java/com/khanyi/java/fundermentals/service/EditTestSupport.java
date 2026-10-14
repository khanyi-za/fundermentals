package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.Track;

import java.time.Duration;

/** Small helpers shared by the service tests. */
final class EditTestSupport {

    private EditTestSupport() {
    }

    static Track track(String id) {
        return track(id, "pop");
    }

    static Track track(String id, String genre) {
        return new Track.Builder()
                .id(id)
                .title(id.toUpperCase())
                .artists("Artist")
                .album("Album")
                .genre(genre)
                .duration(Duration.ofSeconds(60))
                .build();
    }

    /** A playlist holding tracks with the given ids, in order. */
    static Playlist playlistOf(String... ids) {
        Playlist p = new Playlist("p1", "Test");
        for (String id : ids) {
            p.add(track(id));
        }
        return p;
    }

    /** The playlist's ids joined together, e.g. "abc", so a whole state fits in one assertion. */
    static String ids(Playlist p) {
        StringBuilder sb = new StringBuilder();
        for (MediaItem item : p) {
            sb.append(item.getId());
        }
        return sb.toString();
    }
}
