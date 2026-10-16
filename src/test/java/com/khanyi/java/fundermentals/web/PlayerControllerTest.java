package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.khanyi.java.fundermentals.web.WebTestSupport.createPlaylist;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PlayerControllerTest {

    private MockMvc mvc;
    private String abc;   // a playlist holding a, b, c

    @BeforeEach
    void setUp() throws Exception {
        // both controllers share one engine, like the real app
        PlaylistEngineAssembler engine = WebTestSupport.engine();
        mvc = WebTestSupport.mockMvc(new PlaylistController(engine), new PlayerController(engine));
        abc = createPlaylist(mvc, "ABC", "a", "b", "c");
    }

    // ______________ state ______________

    @Test
    void startsIdle() throws Exception {
        mvc.perform(get("/api/player"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nowPlaying").value(nullValue()))
                .andExpect(jsonPath("$.playlistId").value(nullValue()))
                .andExpect(jsonPath("$.shuffle").value("in-order"))
                .andExpect(jsonPath("$.repeat").value("OFF"))
                .andExpect(jsonPath("$.queueSize").value(0))
                .andExpect(jsonPath("$.upNext", hasSize(0)))
                .andExpect(jsonPath("$.recentlyPlayed", hasSize(0)));
    }

    @Test
    void listsTheShuffleStrategies() throws Exception {
        mvc.perform(get("/api/player/shuffle-strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("in-order", "random", "weighted")));
    }

    // ______________ playing ______________

    @Test
    void playStartsAtTheTop() throws Exception {
        mvc.perform(post("/api/player/play/" + abc))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nowPlaying.id").value("a"))
                .andExpect(jsonPath("$.playlistId").value(abc))
                .andExpect(jsonPath("$.upNext[*].id", contains("b", "c")));
    }

    @Test
    void playingAnUnknownPlaylistIs404() throws Exception {
        mvc.perform(post("/api/player/play/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void playingAnEmptyPlaylistIs400() throws Exception {
        String empty = createPlaylist(mvc, "Empty");

        mvc.perform(post("/api/player/play/" + empty))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Playlist 'Empty' is empty"));
    }

    @Test
    void nextWalksThroughAndStopsAtTheEnd() throws Exception {
        mvc.perform(post("/api/player/play/" + abc));

        mvc.perform(post("/api/player/next")).andExpect(jsonPath("$.nowPlaying.id").value("b"));
        mvc.perform(post("/api/player/next")).andExpect(jsonPath("$.nowPlaying.id").value("c"));
        mvc.perform(post("/api/player/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nowPlaying").value(nullValue()))
                .andExpect(jsonPath("$.recentlyPlayed[*].id", contains("c", "b", "a")));
    }

    @Test
    void previousGoesBackAndNextReturns() throws Exception {
        mvc.perform(post("/api/player/play/" + abc));
        mvc.perform(post("/api/player/next"));

        mvc.perform(post("/api/player/previous"))
                .andExpect(jsonPath("$.nowPlaying.id").value("a"))
                .andExpect(jsonPath("$.upNext[0].id").value("b"));
        mvc.perform(post("/api/player/next"))
                .andExpect(jsonPath("$.nowPlaying.id").value("b"));
    }

    @Test
    void previousWithNothingBeforeIs409() throws Exception {
        mvc.perform(post("/api/player/previous"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Nothing was played before this"));
    }

    @Test
    void finishedReplaysWithRepeatOneButNextSkips() throws Exception {
        putJson("/api/player/repeat", "{\"mode\": \"one\"}");
        mvc.perform(post("/api/player/play/" + abc));

        mvc.perform(post("/api/player/finished")).andExpect(jsonPath("$.nowPlaying.id").value("a"));
        mvc.perform(post("/api/player/next")).andExpect(jsonPath("$.nowPlaying.id").value("b"));
    }

    // ______________ Play Next ______________

    @Test
    void queuedTrackPlaysBeforeThePlaylistContinues() throws Exception {
        mvc.perform(post("/api/player/play/" + abc));

        postJson("/api/player/queue", "{\"trackId\": \"e\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueSize").value(1))
                .andExpect(jsonPath("$.upNext[*].id", contains("e", "b", "c")));
        mvc.perform(post("/api/player/next"))
                .andExpect(jsonPath("$.nowPlaying.id").value("e"))
                .andExpect(jsonPath("$.queueSize").value(0));
    }

    @Test
    void queueingAnUnknownTrackIs404AndAMissingIdIs400() throws Exception {
        postJson("/api/player/queue", "{\"trackId\": \"zzz\"}").andExpect(status().isNotFound());
        postJson("/api/player/queue", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("trackId is required"));
    }

    // ______________ shuffle ______________

    @Test
    void shuffleCanBeChosenByName() throws Exception {
        putJson("/api/player/shuffle", "{\"strategy\": \"random\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shuffle").value("random"));
        mvc.perform(post("/api/player/play/" + abc))
                .andExpect(jsonPath("$.upNext", hasSize(2)));
        mvc.perform(get("/api/player"))
                .andExpect(jsonPath("$.shuffle").value("random"));
    }

    @Test
    void shuffledPlaybackStillPlaysEveryTrackOnce() throws Exception {
        putJson("/api/player/shuffle", "{\"strategy\": \"weighted\"}");
        mvc.perform(post("/api/player/play/" + abc));
        mvc.perform(post("/api/player/next"));
        mvc.perform(post("/api/player/next"));

        mvc.perform(post("/api/player/next"))
                .andExpect(jsonPath("$.recentlyPlayed[*].id", containsInAnyOrder("a", "b", "c")));
    }

    @Test
    void unknownShuffleStrategyIs400AndListsTheChoices() throws Exception {
        putJson("/api/player/shuffle", "{\"strategy\": \"alphabetical\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Unknown shuffle strategy 'alphabetical': choose one of [in-order, random, weighted]"));
        putJson("/api/player/shuffle", "{}")
                .andExpect(status().isBadRequest());
    }

    // ______________ repeat ______________

    @Test
    void repeatModeIgnoresCaseAndSpaces() throws Exception {
        putJson("/api/player/repeat", "{\"mode\": \" All \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repeat").value("ALL"));
    }

    @Test
    void repeatAllWrapsRoundButUpNextIsCapped() throws Exception {
        putJson("/api/player/repeat", "{\"mode\": \"all\"}");

        mvc.perform(post("/api/player/play/" + abc))
                .andExpect(jsonPath("$.upNext", hasSize(PlayerController.UP_NEXT_SHOWN)))
                .andExpect(jsonPath("$.upNext[0].id").value("b"))
                .andExpect(jsonPath("$.upNext[2].id").value("a"));
    }

    @Test
    void unknownOrMissingRepeatModeIs400() throws Exception {
        putJson("/api/player/repeat", "{\"mode\": \"sideways\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown repeat mode 'sideways': choose one of [OFF, ALL, ONE]"));
        putJson("/api/player/repeat", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("mode is required: choose one of [OFF, ALL, ONE]"));
    }

    // ______________ shared engine ______________

    @Test
    void playerSeesEditsMadeThroughThePlaylistApi() throws Exception {
        postJson("/api/playlists/" + abc + "/moves", "{\"from\": 2, \"to\": 0}");

        mvc.perform(post("/api/player/play/" + abc))
                .andExpect(jsonPath("$.nowPlaying.id").value("c"));
    }

    @Test
    void recentlyPlayedIsCapped() throws Exception {
        putJson("/api/player/repeat", "{\"mode\": \"all\"}");
        mvc.perform(post("/api/player/play/" + abc));
        for (int i = 0; i < 15; i++) {
            mvc.perform(post("/api/player/next"));
        }

        mvc.perform(get("/api/player"))
                .andExpect(jsonPath("$.recentlyPlayed", hasSize(PlayerController.RECENTLY_PLAYED_SHOWN)));
    }

    // ______________ helpers ______________

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions putJson(String url, String json) throws Exception {
        return mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
