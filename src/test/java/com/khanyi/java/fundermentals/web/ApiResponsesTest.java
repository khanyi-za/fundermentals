package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.PodcastEpisode;
import com.khanyi.java.fundermentals.model.SmartPlaylist;
import com.khanyi.java.fundermentals.model.Track;
import com.khanyi.java.fundermentals.web.ApiResponses.ItemResponse;
import com.khanyi.java.fundermentals.web.ApiResponses.PlaylistResponse;
import com.khanyi.java.fundermentals.web.ApiResponses.TrackResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApiResponsesTest {

    private final Track alpha = WebTestSupport.LIBRARY.get(0);
    private final PodcastEpisode episode =
            new PodcastEpisode("ep1", "Pilot", Duration.ofMinutes(65), "The Show", 1, "Host");

    @Test
    void trackResponseCopiesTheTracksDetails() {
        assertEquals(new TrackResponse("a", "Alpha", List.of("Ann"), "Album a", "jazz", 90, false, 125.0, "3:00"),
                TrackResponse.from(alpha));
    }

    @Test
    void itemResponseWorksForTracksAndEpisodes() {
        assertEquals(new ItemResponse("a", "Track", "Alpha", "Ann", "3:00"), ItemResponse.from(alpha));
        assertEquals(new ItemResponse("ep1", "PodcastEpisode", "Pilot", "The Show", "1:05:00"),
                ItemResponse.from(episode));
    }

    @Test
    void normalPlaylistHasNoRule() {
        Playlist playlist = new Playlist("p1", "Mix");
        playlist.add(alpha);
        playlist.add(episode);

        PlaylistResponse response = PlaylistResponse.from(playlist);

        assertFalse(response.smart());
        assertNull(response.rule());
        assertEquals(2, response.size());
        assertEquals(180 + 65 * 60, response.totalSeconds());
        assertEquals(List.of("a", "ep1"), response.items().stream().map(ItemResponse::id).toList());
    }

    @Test
    void smartPlaylistShowsItsRule() {
        SmartPlaylist jazz = SmartPlaylist.byGenre("p2", "Jazz", "jazz", 5);

        PlaylistResponse response = PlaylistResponse.from(jazz);

        assertTrue(response.smart());
        assertEquals("genre is jazz", response.rule());
    }
}
