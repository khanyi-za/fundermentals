package com.khanyi.java.fundermentals.wiring;

import com.khanyi.java.fundermentals.data.DataSetLoader;
import com.khanyi.java.fundermentals.data.InMemoryTrackRepository;
import com.khanyi.java.fundermentals.data.MalformedRecordException;
import com.khanyi.java.fundermentals.data.SpotifyCsvLoader;
import com.khanyi.java.fundermentals.model.Track;
import com.khanyi.java.fundermentals.service.InOrderStrategy;
import com.khanyi.java.fundermentals.service.PlayerService;
import com.khanyi.java.fundermentals.service.PlaylistService;
import com.khanyi.java.fundermentals.service.RandomShuffle;
import com.khanyi.java.fundermentals.service.ShuffleStrategy;
import com.khanyi.java.fundermentals.service.WeightedByPopularityShuffle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * The composition root: the ONE place that creates the engine's objects and connects them.
 *
 *   tracks --> InMemoryTrackRepository --> PlaylistService --> PlayerService
 *                                     |____________________________^
 *
 * Every dependency is passed in with plain `new` (constructor injection) or a setter
 * (setter injection). No other class in the engine creates its own collaborators,
 * and no class here needs Spring. This is the wiring Spring's @Autowired would hide.
 */
public final class PlaylistEngineAssembler {

    private static final Logger log = LoggerFactory.getLogger(PlaylistEngineAssembler.class);

    public static final String IN_ORDER = "in-order";
    public static final String RANDOM = "random";
    public static final String WEIGHTED = "weighted";

    private final InMemoryTrackRepository trackRepository;
    private final PlaylistService playlistService;
    private final PlayerService playerService;
    private final Map<String, ShuffleStrategy> shuffleStrategies;

    /**
     * Wires an engine around tracks that are already loaded.
     *
     * @param library the tracks to serve
     * @param random  the source of randomness for every shuffle (pass a seeded one in tests)
     */
    public PlaylistEngineAssembler(Collection<Track> library, Random random) {
        if (library == null || random == null) {
            throw new IllegalArgumentException("library and random must not be null");
        }

        // 1. data
        trackRepository = new InMemoryTrackRepository(library);

        // 2. services: constructor injection
        playlistService = new PlaylistService(trackRepository);
        playerService = new PlayerService(playlistService, trackRepository);

        // 3. strategies, sharing one Random, offered by name
        Map<String, ShuffleStrategy> strategies = new LinkedHashMap<>();
        strategies.put(IN_ORDER, new InOrderStrategy());
        strategies.put(RANDOM, new RandomShuffle(random));
        strategies.put(WEIGHTED, new WeightedByPopularityShuffle(random));
        shuffleStrategies = Collections.unmodifiableMap(strategies);

        // 4. setter injection: the player's starting strategy
        playerService.setShuffleStrategy(shuffleStrategies.get(IN_ORDER));

        log.info("Playlist engine ready: {} tracks, shuffle strategies {}",
                trackRepository.count(), shuffleStrategies.keySet());
    }

    /** Loads the Spotify CSV and wires the engine around it, with a real (unseeded) Random. */
    public static PlaylistEngineAssembler fromSpotifyCsv(Path csv) throws IOException, MalformedRecordException {
        DataSetLoader<Track> loader = new SpotifyCsvLoader();
        return new PlaylistEngineAssembler(loader.load(csv), new Random());
    }

    // ______________ what the web layer will use ______________

    public InMemoryTrackRepository trackRepository() {
        return trackRepository;
    }

    public PlaylistService playlistService() {
        return playlistService;
    }

    public PlayerService playerService() {
        return playerService;
    }

    /** The names a client can choose from: "in-order", "random", "weighted". */
    public Set<String> shuffleStrategyNames() {
        return shuffleStrategies.keySet();
    }

    /** @return the strategy with this name, or empty if there's none */
    public Optional<ShuffleStrategy> shuffleStrategy(String name) {
        return Optional.ofNullable(shuffleStrategies.get(name));
    }
}
