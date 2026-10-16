package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.data.InMemoryTrackRepository;
import com.khanyi.java.fundermentals.data.Repository;
import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.SmartPlaylist;
import com.khanyi.java.fundermentals.model.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Everything a user can do with playlists: create, edit (with undo/redo), refresh and delete.
 *
 * Dependencies come in through the constructor (constructor injection), so this class
 * never decides for itself where tracks come from or how ids are made. The web layer
 * (Step 10) will call this class; it knows nothing about HTTP.
 */
public class PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistService.class);

    private final Repository<Track, String> tracks;
    private final Supplier<String> idGenerator;

    private final Map<String, Playlist> playlists = new LinkedHashMap<>();   // keeps creation order
    private final Map<String, EditHistory> histories = new HashMap<>();

    /**
     * @param tracks      where to look tracks up by id
     * @param idGenerator makes a new, unique playlist id each time it is called
     */
    public PlaylistService(Repository<Track, String> tracks, Supplier<String> idGenerator) {
        if (tracks == null || idGenerator == null) {
            throw new IllegalArgumentException("tracks and idGenerator must not be null");
        }
        this.tracks = tracks;
        this.idGenerator = idGenerator;
    }

    /** Uses random UUIDs for playlist ids. */
    public PlaylistService(Repository<Track, String> tracks) {
        this(tracks, () -> UUID.randomUUID().toString());
    }

    // ______________ creating and finding ______________

    public Playlist createPlaylist(String name) {
        return register(new Playlist(idGenerator.get(), name));
    }

    /** Creates a smart playlist of the most popular tracks in a genre, already filled. */
    public SmartPlaylist createGenrePlaylist(String name, String genre, int limit) {
        SmartPlaylist playlist = register(SmartPlaylist.byGenre(idGenerator.get(), name, genre, limit));
        playlist.refresh(libraryByPopularity());
        return playlist;
    }

    /** Creates a smart playlist of the most popular tracks in a tempo range, already filled. */
    public SmartPlaylist createTempoPlaylist(String name, double minBpm, double maxBpm, int limit) {
        SmartPlaylist playlist = register(SmartPlaylist.byTempo(idGenerator.get(), name, minBpm, maxBpm, limit));
        playlist.refresh(libraryByPopularity());
        return playlist;
    }

    public Optional<Playlist> findPlaylist(String playlistId) {
        return Optional.ofNullable(playlists.get(playlistId));
    }

    /** @throws NoSuchElementException if there is no playlist with this id */
    public Playlist getPlaylist(String playlistId) {
        return findPlaylist(playlistId)
                .orElseThrow(() -> new NoSuchElementException("No playlist with id '" + playlistId + "'"));
    }

    /** @return every playlist, oldest first, as a list the caller owns */
    public List<Playlist> allPlaylists() {
        return new ArrayList<>(playlists.values());
    }

    public void renamePlaylist(String playlistId, String newName) {
        getPlaylist(playlistId).rename(newName);
    }

    public void deletePlaylist(String playlistId) {
        Playlist removed = getPlaylist(playlistId);
        playlists.remove(playlistId);
        histories.remove(playlistId);
        log.info("Deleted playlist {} '{}'", playlistId, removed.getName());
    }

    // ______________ editing (all undoable) ______________
    /** Adds a track from the library to the end of the playlist. */
    public void addTrack(String playlistId, String trackId) {
        Playlist playlist = getPlaylist(playlistId);
        addTrack(playlistId, trackId, playlist.size());
    }

    /** @throws com.khanyi.java.fundermentals.data.TrackNotFoundException if the track isn't in the library */
    public void addTrack(String playlistId, String trackId, int index) {
        Playlist playlist = getPlaylist(playlistId);
        Track track = tracks.getById(trackId);
        historyOf(playlistId).perform(new AddItemEdit(playlist, index, track));
    }

    /** Adds any item, e.g. a PodcastEpisode, which isn't in the track library. */
    public void addItem(String playlistId, MediaItem item) {
        Playlist playlist = getPlaylist(playlistId);
        historyOf(playlistId).perform(new AddItemEdit(playlist, playlist.size(), item));
    }

    /** @return the item that was removed */
    public MediaItem removeItem(String playlistId, int index) {
        Playlist playlist = getPlaylist(playlistId);
        MediaItem item = playlist.get(index);   // also checks the index before anything is recorded
        historyOf(playlistId).perform(new RemoveItemEdit(playlist, index));
        return item;
    }

    public void moveItem(String playlistId, int from, int to) {
        Playlist playlist = getPlaylist(playlistId);
        if (from == to) {
            Objects.checkIndex(from, playlist.size());   // still reject a bad index...
            return;                                       // ...but don't fill the history with no-ops
        }
        historyOf(playlistId).perform(new MoveItemEdit(playlist, from, to));
    }

    // ______________ undo and redo ______________

    /** @return a description of what was undone, e.g. "add 'Comedy' at 3" */
    public String undo(String playlistId) {
        getPlaylist(playlistId);
        return historyOf(playlistId).undo().description();
    }

    /** @return a description of what was redone */
    public String redo(String playlistId) {
        getPlaylist(playlistId);
        return historyOf(playlistId).redo().description();
    }

    public Optional<String> nextUndo(String playlistId) {
        getPlaylist(playlistId);
        return historyOf(playlistId).nextUndo();
    }

    public Optional<String> nextRedo(String playlistId) {
        getPlaylist(playlistId);
        return historyOf(playlistId).nextRedo();
    }

    // ______________ smart playlists ______________

    /**
     * Refills a smart playlist from the current library. Its undo history is cleared,
     * because the old edits point at indexes that no longer hold the same tracks.
     *
     * @return how many tracks it now holds
     * @throws IllegalArgumentException if the playlist isn't a smart playlist
     */
    public int refreshSmartPlaylist(String playlistId) {
        Playlist playlist = getPlaylist(playlistId);
        if (!(playlist instanceof SmartPlaylist smart)) {
            throw new IllegalArgumentException("Playlist '" + playlist.getName() + "' is not a smart playlist");
        }
        int count = smart.refresh(libraryByPopularity());
        historyOf(playlistId).clear();
        log.info("Refreshed smart playlist {} '{}': {} tracks", playlistId, smart.getName(), count);
        return count;
    }

    // ______________ helpers ______________

    private <P extends Playlist> P register(P playlist) {
        if (playlists.containsKey(playlist.getId())) {
            throw new IllegalStateException("idGenerator gave a duplicate id: " + playlist.getId());
        }
        playlists.put(playlist.getId(), playlist);
        histories.put(playlist.getId(), new EditHistory());
        log.info("Created {} {} '{}'", playlist.getClass().getSimpleName(), playlist.getId(), playlist.getName());
        return playlist;
    }

    private EditHistory historyOf(String playlistId) {
        return histories.get(playlistId);
    }

    /** A copy of the library, most popular first, so smart playlists pick the best matches. */
    private List<Track> libraryByPopularity() {
        List<Track> library = new ArrayList<>(tracks.findAll());
        library.sort(InMemoryTrackRepository.MOST_POPULAR_FIRST);
        return library;
    }
}
