package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.service.PlayerService;
import com.khanyi.java.fundermentals.service.RepeatMode;
import com.khanyi.java.fundermentals.service.ShuffleStrategy;
import com.khanyi.java.fundermentals.web.ApiRequests.QueueTrack;
import com.khanyi.java.fundermentals.web.ApiRequests.RepeatChoice;
import com.khanyi.java.fundermentals.web.ApiRequests.ShuffleChoice;
import com.khanyi.java.fundermentals.web.ApiResponses.ItemResponse;
import com.khanyi.java.fundermentals.web.ApiResponses.PlayerResponse;
import com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

/**
 * The player. Every call returns the whole player state, so a client never has to
 * ask twice.
 *
 *   GET  /api/player                       the current state
 *   POST /api/player/play/{playlistId}
 *   POST /api/player/next                  skip
 *   POST /api/player/finished              the song ended on its own (repeat ONE replays it)
 *   POST /api/player/previous              409 if nothing was played before
 *   POST /api/player/queue                 {"trackId"}  Play Next
 *   PUT  /api/player/shuffle               {"strategy": "in-order" | "random" | "weighted"}
 *   PUT  /api/player/repeat                {"mode": "off" | "all" | "one"}
 *   GET  /api/player/shuffle-strategies    the names you can choose from
 *
 * Locks the same shared engine as PlaylistController, because playing reads playlists.
 */
@RestController
@RequestMapping("/api/player")
public class PlayerController {

    static final int UP_NEXT_SHOWN = 10;
    static final int RECENTLY_PLAYED_SHOWN = 10;

    private final PlaylistEngineAssembler engine;
    private final PlayerService player;

    public PlayerController(PlaylistEngineAssembler engine) {
        this.engine = engine;
        this.player = engine.playerService();
    }

    @GetMapping
    public PlayerResponse state() {
        synchronized (engine) {
            return snapshot();
        }
    }

    @PostMapping("/play/{playlistId}")
    public PlayerResponse play(@PathVariable String playlistId) {
        synchronized (engine) {
            player.play(playlistId);
            return snapshot();
        }
    }

    @PostMapping("/next")
    public PlayerResponse next() {
        synchronized (engine) {
            player.next();   // empty at the end of the playlist: the snapshot then shows nothing playing
            return snapshot();
        }
    }

    @PostMapping("/finished")
    public PlayerResponse finished() {
        synchronized (engine) {
            player.trackFinished();
            return snapshot();
        }
    }

    @PostMapping("/previous")
    public PlayerResponse previous() {
        synchronized (engine) {
            player.previous();
            return snapshot();
        }
    }

    @PostMapping("/queue")
    public PlayerResponse queue(@RequestBody QueueTrack request) {
        if (request.trackId() == null || request.trackId().isBlank()) {
            throw new IllegalArgumentException("trackId is required");
        }
        synchronized (engine) {
            player.playNext(request.trackId());
            return snapshot();
        }
    }

    @PutMapping("/shuffle")
    public PlayerResponse shuffle(@RequestBody ShuffleChoice request) {
        ShuffleStrategy chosen = engine.shuffleStrategy(request.strategy())
                .orElseThrow(() -> new IllegalArgumentException("Unknown shuffle strategy '" + request.strategy()
                        + "': choose one of " + engine.shuffleStrategyNames()));
        synchronized (engine) {
            player.setShuffleStrategy(chosen);
            return snapshot();
        }
    }

    @PutMapping("/repeat")
    public PlayerResponse repeat(@RequestBody RepeatChoice request) {
        RepeatMode mode = parseRepeatMode(request.mode());
        synchronized (engine) {
            player.setRepeatMode(mode);
            return snapshot();
        }
    }

    @GetMapping("/shuffle-strategies")
    public Set<String> shuffleStrategies() {
        return engine.shuffleStrategyNames();
    }

    // ______________ helpers ______________

    /** Reads the player's state. Callers must hold the engine lock. */
    private PlayerResponse snapshot() {
        return new PlayerResponse(
                player.nowPlaying().map(ItemResponse::from).orElse(null),
                player.currentPlaylist().map(Playlist::getId).orElse(null),
                shuffleName(player.getShuffleStrategy()),
                player.getRepeatMode().name(),
                player.queueSize(),
                ItemResponse.fromAll(player.upNext(UP_NEXT_SHOWN)),
                ItemResponse.fromAll(player.recentlyPlayed().stream().limit(RECENTLY_PLAYED_SHOWN).toList()));
    }

    /** The name the client used to choose this strategy, e.g. "weighted". */
    private String shuffleName(ShuffleStrategy current) {
        for (String name : engine.shuffleStrategyNames()) {
            if (engine.shuffleStrategy(name).orElseThrow() == current) {
                return name;
            }
        }
        return current.name();   // not one of the assembler's; can't happen through this API
    }

    /** "all", "ALL" or " All " -> RepeatMode.ALL, with a clear message for anything else. */
    private static RepeatMode parseRepeatMode(String mode) {
        String choices = Arrays.toString(RepeatMode.values());
        if (mode == null) {
            throw new IllegalArgumentException("mode is required: choose one of " + choices);
        }
        try {
            return RepeatMode.valueOf(mode.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown repeat mode '" + mode + "': choose one of " + choices);
        }
    }
}
