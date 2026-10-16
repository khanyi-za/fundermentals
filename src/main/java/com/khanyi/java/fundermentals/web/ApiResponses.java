package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.SmartPlaylist;
import com.khanyi.java.fundermentals.model.Track;

import java.util.List;

/**
 * The JSON the API sends back. The domain objects are never serialized directly:
 * these records decide exactly which fields a client sees, so the model can change
 * without breaking the API.
 */
public final class ApiResponses {

    private ApiResponses() {
    }

    /** A track from the library, with its details. */
    public record TrackResponse(String id, String title, List<String> artists, String album, String genre,
                                int popularity, boolean explicit, double tempo, String duration) {

        static TrackResponse from(Track track) {
            return new TrackResponse(track.getId(), track.getTitle(), track.getArtists(), track.getAlbum(),
                    track.getGenre(), track.getPopularity(), track.isExplicit(), track.getTempo(),
                    track.formattedDuration());
        }
    }

    /** Any playable item (track or episode) as it appears in a playlist or the player. */
    public record ItemResponse(String id, String type, String title, String creator, String duration) {

        static ItemResponse from(MediaItem item) {
            return new ItemResponse(item.getId(), item.getClass().getSimpleName(), item.getTitle(),
                    item.getCreator(), item.formattedDuration());
        }

        static List<ItemResponse> fromAll(List<? extends MediaItem> items) {
            return items.stream().map(ItemResponse::from).toList();
        }
    }

    /** A playlist and its items. {@code rule} is null for a normal playlist. */
    public record PlaylistResponse(String id, String name, boolean smart, String rule, int size,
                                   long totalSeconds, List<ItemResponse> items) {

        static PlaylistResponse from(Playlist playlist) {
            String rule = playlist instanceof SmartPlaylist smart ? smart.getRuleDescription() : null;
            return new PlaylistResponse(playlist.getId(), playlist.getName(), rule != null, rule,
                    playlist.size(), playlist.getTotalDuration().toSeconds(),
                    ItemResponse.fromAll(playlist.getItems()));
        }
    }

    /**
     * A playlist's undo/redo state. {@code lastChange} says what an undo or redo just did,
     * and is null when the history was only looked at.
     */
    public record HistoryResponse(String lastChange, String nextUndo, String nextRedo) {
    }

    /** Everything a player screen shows. */
    public record PlayerResponse(ItemResponse nowPlaying, String playlistId, String shuffle, String repeat,
                                 int queueSize, List<ItemResponse> upNext, List<ItemResponse> recentlyPlayed) {
    }
}
