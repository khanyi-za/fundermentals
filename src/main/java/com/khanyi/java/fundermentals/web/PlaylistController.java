package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.service.PlaylistService;
import com.khanyi.java.fundermentals.web.ApiRequests.AddTrack;
import com.khanyi.java.fundermentals.web.ApiRequests.Move;
import com.khanyi.java.fundermentals.web.ApiRequests.NewGenrePlaylist;
import com.khanyi.java.fundermentals.web.ApiRequests.NewPlaylist;
import com.khanyi.java.fundermentals.web.ApiRequests.NewTempoPlaylist;
import com.khanyi.java.fundermentals.web.ApiRequests.Rename;
import com.khanyi.java.fundermentals.web.ApiResponses.HistoryResponse;
import com.khanyi.java.fundermentals.web.ApiResponses.PlaylistResponse;
import com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Playlists: create, edit, undo and redo.
 *
 *   GET    /api/playlists                    all playlists
 *   POST   /api/playlists                    {"name"}                          -> 201
 *   POST   /api/playlists/genre              {"name", "genre", "limit"}        -> 201
 *   POST   /api/playlists/tempo              {"name", "minBpm", "maxBpm", "limit"} -> 201
 *   GET    /api/playlists/{id}
 *   PATCH  /api/playlists/{id}               {"name"}  rename
 *   DELETE /api/playlists/{id}                                                 -> 204
 *   POST   /api/playlists/{id}/tracks        {"trackId", "index"?}
 *   DELETE /api/playlists/{id}/items/{index}
 *   POST   /api/playlists/{id}/moves         {"from", "to"}
 *   POST   /api/playlists/{id}/refresh       refill a smart playlist
 *   GET    /api/playlists/{id}/history       what undo/redo would do
 *   POST   /api/playlists/{id}/undo
 *   POST   /api/playlists/{id}/redo
 *
 * Thread safety: Spring serves requests on many threads, but PlaylistService is not
 * thread-safe. Every method locks the whole engine (synchronized on the one shared
 * assembler), so only one request at a time reads or changes playlists or the player.
 * The response is built INSIDE the lock, so it never shows a half-finished edit.
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistEngineAssembler engine;
    private final PlaylistService playlists;

    public PlaylistController(PlaylistEngineAssembler engine) {
        this.engine = engine;
        this.playlists = engine.playlistService();
    }

    // ______________ creating ______________

    @GetMapping
    public List<PlaylistResponse> all() {
        synchronized (engine) {
            return playlists.allPlaylists().stream().map(PlaylistResponse::from).toList();
        }
    }

    @PostMapping
    public ResponseEntity<PlaylistResponse> create(@RequestBody NewPlaylist request) {
        synchronized (engine) {
            return created(playlists.createPlaylist(request.name()));
        }
    }

    @PostMapping("/genre")
    public ResponseEntity<PlaylistResponse> createGenre(@RequestBody NewGenrePlaylist request) {
        synchronized (engine) {
            return created(playlists.createGenrePlaylist(request.name(), request.genre(), request.limit()));
        }
    }

    @PostMapping("/tempo")
    public ResponseEntity<PlaylistResponse> createTempo(@RequestBody NewTempoPlaylist request) {
        synchronized (engine) {
            return created(playlists.createTempoPlaylist(
                    request.name(), request.minBpm(), request.maxBpm(), request.limit()));
        }
    }

    // ______________ one playlist ______________

    @GetMapping("/{id}")
    public PlaylistResponse get(@PathVariable String id) {
        synchronized (engine) {
            return PlaylistResponse.from(playlists.getPlaylist(id));
        }
    }

    @PatchMapping("/{id}")
    public PlaylistResponse rename(@PathVariable String id, @RequestBody Rename request) {
        synchronized (engine) {
            playlists.renamePlaylist(id, request.name());
            return PlaylistResponse.from(playlists.getPlaylist(id));
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        synchronized (engine) {
            playlists.deletePlaylist(id);
        }
    }

    // ______________ editing (all undoable) ______________

    @PostMapping("/{id}/tracks")
    public PlaylistResponse addTrack(@PathVariable String id, @RequestBody AddTrack request) {
        if (request.trackId() == null || request.trackId().isBlank()) {
            throw new IllegalArgumentException("trackId is required");
        }
        synchronized (engine) {
            if (request.index() == null) {
                playlists.addTrack(id, request.trackId());
            } else {
                playlists.addTrack(id, request.trackId(), request.index());
            }
            return PlaylistResponse.from(playlists.getPlaylist(id));
        }
    }

    @DeleteMapping("/{id}/items/{index}")
    public PlaylistResponse removeItem(@PathVariable String id, @PathVariable int index) {
        synchronized (engine) {
            playlists.removeItem(id, index);
            return PlaylistResponse.from(playlists.getPlaylist(id));
        }
    }

    @PostMapping("/{id}/moves")
    public PlaylistResponse move(@PathVariable String id, @RequestBody Move request) {
        synchronized (engine) {
            playlists.moveItem(id, request.from(), request.to());
            return PlaylistResponse.from(playlists.getPlaylist(id));
        }
    }

    @PostMapping("/{id}/refresh")
    public PlaylistResponse refresh(@PathVariable String id) {
        synchronized (engine) {
            playlists.refreshSmartPlaylist(id);
            return PlaylistResponse.from(playlists.getPlaylist(id));
        }
    }

    // ______________ undo and redo ______________

    @GetMapping("/{id}/history")
    public HistoryResponse history(@PathVariable String id) {
        synchronized (engine) {
            return history(id, null);
        }
    }

    @PostMapping("/{id}/undo")
    public HistoryResponse undo(@PathVariable String id) {
        synchronized (engine) {
            return history(id, "Undid: " + playlists.undo(id));
        }
    }

    @PostMapping("/{id}/redo")
    public HistoryResponse redo(@PathVariable String id) {
        synchronized (engine) {
            return history(id, "Redid: " + playlists.redo(id));
        }
    }

    // ______________ helpers ______________

    private HistoryResponse history(String id, String lastChange) {
        return new HistoryResponse(lastChange,
                playlists.nextUndo(id).orElse(null),
                playlists.nextRedo(id).orElse(null));
    }

    /** 201 Created, with a Location header saying where the new playlist lives. */
    private static ResponseEntity<PlaylistResponse> created(Playlist playlist) {
        return ResponseEntity.created(URI.create("/api/playlists/" + playlist.getId()))
                .body(PlaylistResponse.from(playlist));
    }
}
