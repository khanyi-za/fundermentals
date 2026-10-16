package com.khanyi.java.fundermentals.web;

import com.jayway.jsonpath.JsonPath;
import com.khanyi.java.fundermentals.model.Track;
import com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.util.List;
import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared set-up for the controller tests.
 *
 * Each test gets a FRESH engine (seeded Random) and a MockMvc built around real controllers
 * with standaloneSetup: no Spring context, so tests are fast and can't leak playlists or
 * player state into each other. FundermentalsApplicationTests checks the real Spring wiring.
 */
final class WebTestSupport {

    private WebTestSupport() {
    }

    /**
     *  id  title         genre  popularity  tempo  length
     *  a   Alpha         jazz   90          125    3:00
     *  b   Bravo         jazz   80           95    2:00
     *  c   Charlie       rock   70          128    4:00
     *  d   Delta Blues   blues  60          100    1:30
     *  e   Echo          jazz   50          122    2:30
     */
    static final List<Track> LIBRARY = List.of(
            track("a", "Alpha", "Ann", "jazz", 90, 125, 180),
            track("b", "Bravo", "Ben", "jazz", 80, 95, 120),
            track("c", "Charlie", "Cat", "rock", 70, 128, 240),
            track("d", "Delta Blues", "Dan", "blues", 60, 100, 90),
            track("e", "Echo", "Eve", "jazz", 50, 122, 150));

    static PlaylistEngineAssembler engine() {
        return new PlaylistEngineAssembler(LIBRARY, new Random(42));
    }

    /** MockMvc around these controllers, with the real exception handler. */
    static MockMvc mockMvc(Object... controllers) {
        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    /** Creates a playlist through the API, adds these tracks, and returns its id. */
    static String createPlaylist(MockMvc mvc, String name, String... trackIds) throws Exception {
        String json = mvc.perform(post("/api/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.id");
        for (String trackId : trackIds) {
            mvc.perform(post("/api/playlists/" + id + "/tracks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"trackId\": \"" + trackId + "\"}"))
                    .andExpect(status().isOk());
        }
        return id;
    }

    private static Track track(String id, String title, String artist, String genre,
                               int popularity, double tempo, int seconds) {
        return new Track.Builder()
                .id(id)
                .title(title)
                .artists(artist)
                .album("Album " + id)
                .genre(genre)
                .popularity(popularity)
                .tempo(tempo)
                .duration(Duration.ofSeconds(seconds))
                .build();
    }
}
