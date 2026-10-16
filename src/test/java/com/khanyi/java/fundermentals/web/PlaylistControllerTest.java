package com.khanyi.java.fundermentals.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.khanyi.java.fundermentals.web.WebTestSupport.createPlaylist;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PlaylistControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = WebTestSupport.mockMvc(new PlaylistController(WebTestSupport.engine()));
    }

    // ______________ creating ______________

    @Test
    void createReturns201WithALocationHeader() throws Exception {
        postJson("/api/playlists", "{\"name\": \"Road trip\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/playlists/")))
                .andExpect(jsonPath("$.name").value("Road trip"))
                .andExpect(jsonPath("$.smart").value(false))
                .andExpect(jsonPath("$.rule").value(nullValue()))
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void theLocationHeaderPointsAtTheNewPlaylist() throws Exception {
        String location = postJson("/api/playlists", "{\"name\": \"Mine\"}")
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mine"));
    }

    @Test
    void blankOrMissingNameIs400() throws Exception {
        postJson("/api/playlists", "{\"name\": \"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("name must not be blank"));
        postJson("/api/playlists", "{}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        postJson("/api/playlists", "{not json")
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsAllPlaylistsOldestFirst() throws Exception {
        createPlaylist(mvc, "First");
        createPlaylist(mvc, "Second");

        mvc.perform(get("/api/playlists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("First", "Second")));
    }

    @Test
    void genrePlaylistIsFilledMostPopularFirst() throws Exception {
        postJson("/api/playlists/genre", "{\"name\": \"Jazz\", \"genre\": \"jazz\", \"limit\": 2}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.smart").value(true))
                .andExpect(jsonPath("$.rule").value("genre is jazz"))
                .andExpect(jsonPath("$.items[*].id", contains("a", "b")))
                .andExpect(jsonPath("$.totalSeconds").value(300));
    }

    @Test
    void genrePlaylistWithoutALimitIs400() throws Exception {
        // limit is an int: Jackson refuses to make up a value for a missing primitive
        postJson("/api/playlists/genre", "{\"name\": \"Jazz\", \"genre\": \"jazz\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void tempoPlaylistTakesTracksInTheRange() throws Exception {
        postJson("/api/playlists/tempo", "{\"name\": \"Gym\", \"minBpm\": 120, \"maxBpm\": 130, \"limit\": 10}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rule").value("tempo 120.0 to 130.0 BPM"))
                .andExpect(jsonPath("$.items[*].id", contains("a", "c", "e")));
    }

    // ______________ one playlist ______________

    @Test
    void getShowsTheItemsInOrder() throws Exception {
        String id = createPlaylist(mvc, "Mix", "c", "a");

        mvc.perform(get("/api/playlists/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalSeconds").value(420))
                .andExpect(jsonPath("$.items[0].id").value("c"))
                .andExpect(jsonPath("$.items[0].type").value("Track"))
                .andExpect(jsonPath("$.items[0].title").value("Charlie"))
                .andExpect(jsonPath("$.items[0].creator").value("Cat"))
                .andExpect(jsonPath("$.items[0].duration").value("4:00"));
    }

    @Test
    void unknownPlaylistIs404() throws Exception {
        mvc.perform(get("/api/playlists/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No playlist with id 'nope'"));
    }

    @Test
    void renameChangesTheName() throws Exception {
        String id = createPlaylist(mvc, "Old");

        mvc.perform(patch("/api/playlists/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"New\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void renameToABlankNameIs400() throws Exception {
        String id = createPlaylist(mvc, "Old");

        mvc.perform(patch("/api/playlists/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204AndThePlaylistIsGone() throws Exception {
        String id = createPlaylist(mvc, "Short-lived");

        mvc.perform(delete("/api/playlists/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/playlists/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/playlists/" + id)).andExpect(status().isNotFound());
    }

    // ______________ editing ______________

    @Test
    void addTrackAddsAtTheEndOrAtAnIndex() throws Exception {
        String id = createPlaylist(mvc, "Mix", "a", "b");

        postJson("/api/playlists/" + id + "/tracks", "{\"trackId\": \"c\", \"index\": 0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains("c", "a", "b")));
        postJson("/api/playlists/" + id + "/tracks", "{\"trackId\": \"d\"}")
                .andExpect(jsonPath("$.items[*].id", contains("c", "a", "b", "d")));
    }

    @Test
    void addingAnUnknownTrackIs404() throws Exception {
        String id = createPlaylist(mvc, "Mix");

        postJson("/api/playlists/" + id + "/tracks", "{\"trackId\": \"zzz\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No track with id 'zzz'"));
    }

    @Test
    void addingWithoutATrackIdIs400() throws Exception {
        String id = createPlaylist(mvc, "Mix");

        postJson("/api/playlists/" + id + "/tracks", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("trackId is required"));
    }

    @Test
    void addingAtABadIndexIs400() throws Exception {
        String id = createPlaylist(mvc, "Mix", "a");

        postJson("/api/playlists/" + id + "/tracks", "{\"trackId\": \"b\", \"index\": 5}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void addingANonMatchingTrackToASmartPlaylistIs400() throws Exception {
        String json = postJson("/api/playlists/genre", "{\"name\": \"Jazz\", \"genre\": \"jazz\", \"limit\": 5}")
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(json, "$.id");

        postJson("/api/playlists/" + id + "/tracks", "{\"trackId\": \"c\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("'Charlie' doesn't match this playlist's rule: genre is jazz"));
    }

    @Test
    void removeItemTakesItOut() throws Exception {
        String id = createPlaylist(mvc, "Mix", "a", "b", "c");

        mvc.perform(delete("/api/playlists/" + id + "/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains("a", "c")));
        mvc.perform(delete("/api/playlists/" + id + "/items/9"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moveTakesAnItemToItsNewPlace() throws Exception {
        String id = createPlaylist(mvc, "Mix", "a", "b", "c");

        postJson("/api/playlists/" + id + "/moves", "{\"from\": 2, \"to\": 0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains("c", "a", "b")));
        postJson("/api/playlists/" + id + "/moves", "{\"from\": 0, \"to\": 3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Index 3 out of bounds for length 3"));
    }

    @Test
    void editingAnUnknownPlaylistIs404() throws Exception {
        postJson("/api/playlists/nope/tracks", "{\"trackId\": \"a\"}").andExpect(status().isNotFound());
        postJson("/api/playlists/nope/moves", "{\"from\": 0, \"to\": 1}").andExpect(status().isNotFound());
        mvc.perform(delete("/api/playlists/nope/items/0")).andExpect(status().isNotFound());
    }

    // ______________ smart playlists ______________

    @Test
    void refreshRefillsASmartPlaylist() throws Exception {
        String json = postJson("/api/playlists/genre", "{\"name\": \"Jazz\", \"genre\": \"jazz\", \"limit\": 3}")
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(json, "$.id");
        mvc.perform(delete("/api/playlists/" + id + "/items/0"));

        mvc.perform(post("/api/playlists/" + id + "/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains("a", "b", "e")));
    }

    @Test
    void refreshingANormalPlaylistIs400() throws Exception {
        String id = createPlaylist(mvc, "Plain");

        mvc.perform(post("/api/playlists/" + id + "/refresh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Playlist 'Plain' is not a smart playlist"));
    }

    // ______________ undo and redo ______________

    @Test
    void historyOfANewPlaylistIsEmpty() throws Exception {
        String id = createPlaylist(mvc, "Mix");

        mvc.perform(get("/api/playlists/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastChange").value(nullValue()))
                .andExpect(jsonPath("$.nextUndo").value(nullValue()))
                .andExpect(jsonPath("$.nextRedo").value(nullValue()));
    }

    @Test
    void undoAndRedoAMove() throws Exception {
        String id = createPlaylist(mvc, "Mix", "a", "b", "c");
        postJson("/api/playlists/" + id + "/moves", "{\"from\": 2, \"to\": 0}");

        mvc.perform(post("/api/playlists/" + id + "/undo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastChange").value("Undid: move item 2 to 0"))
                .andExpect(jsonPath("$.nextUndo").value("add 'Charlie' at 2"))
                .andExpect(jsonPath("$.nextRedo").value("move item 2 to 0"));
        mvc.perform(get("/api/playlists/" + id))
                .andExpect(jsonPath("$.items[*].id", contains("a", "b", "c")));

        mvc.perform(post("/api/playlists/" + id + "/redo"))
                .andExpect(jsonPath("$.lastChange").value("Redid: move item 2 to 0"))
                .andExpect(jsonPath("$.nextRedo").value(nullValue()));
        mvc.perform(get("/api/playlists/" + id))
                .andExpect(jsonPath("$.items[*].id", contains("c", "a", "b")));
    }

    @Test
    void undoWithNothingToUndoIs409() throws Exception {
        String id = createPlaylist(mvc, "Mix");

        mvc.perform(post("/api/playlists/" + id + "/undo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Nothing to undo"));
        mvc.perform(post("/api/playlists/" + id + "/redo"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Nothing to redo"));
    }

    @Test
    void undoOnAnUnknownPlaylistIs404() throws Exception {
        mvc.perform(post("/api/playlists/nope/undo")).andExpect(status().isNotFound());
        mvc.perform(get("/api/playlists/nope/history")).andExpect(status().isNotFound());
    }

    // ______________ helpers ______________

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
