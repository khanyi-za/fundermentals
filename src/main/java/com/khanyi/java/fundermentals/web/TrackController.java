package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.data.InMemoryTrackRepository;
import com.khanyi.java.fundermentals.web.ApiResponses.TrackResponse;
import com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only access to the track library.
 *
 *   GET /api/tracks?q=adele&limit=20   search titles, artists, albums and genres
 *   GET /api/tracks/top?limit=10       the most popular tracks
 *   GET /api/tracks/{id}               one track (404 if there's none)
 *
 * No lock is needed here: nothing changes the library after startup,
 * and many threads may safely read a collection that nobody writes to.
 */
@RestController
@RequestMapping("/api/tracks")
public class TrackController {

    static final int MAX_LIMIT = 100;

    private final InMemoryTrackRepository tracks;

    public TrackController(PlaylistEngineAssembler engine) {
        this.tracks = engine.trackRepository();
    }

    @GetMapping
    public List<TrackResponse> search(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(defaultValue = "20") int limit) {
        checkLimit(limit);
        return tracks.search(q).stream()
                .limit(limit)
                .map(TrackResponse::from)
                .toList();
    }

    @GetMapping("/top")
    public List<TrackResponse> top(@RequestParam(defaultValue = "10") int limit) {
        checkLimit(limit);
        return tracks.topByPopularity(limit).stream()
                .map(TrackResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TrackResponse byId(@PathVariable String id) {
        return TrackResponse.from(tracks.getById(id));
    }

    /** Keeps one request from asking for all 89,740 tracks at once. */
    private static void checkLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT + ", was " + limit);
        }
    }
}
