package com.khanyi.java.fundermentals.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TrackControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = WebTestSupport.mockMvc(new TrackController(WebTestSupport.engine()));
    }

    // ______________ search ______________

    @Test
    void searchMatchesTitlesIgnoringCase() throws Exception {
        mvc.perform(get("/api/tracks").param("q", "BLUES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains("d")));
    }

    @Test
    void searchMatchesGenresAndArtists() throws Exception {
        mvc.perform(get("/api/tracks").param("q", "jazz"))
                .andExpect(jsonPath("$[*].id", contains("a", "b", "e")));
        mvc.perform(get("/api/tracks").param("q", "eve"))
                .andExpect(jsonPath("$[*].id", contains("e")));
    }

    @Test
    void searchWithNoQueryListsTheLibraryUpToTheLimit() throws Exception {
        mvc.perform(get("/api/tracks"))
                .andExpect(jsonPath("$", hasSize(5)));
        mvc.perform(get("/api/tracks").param("limit", "2"))
                .andExpect(jsonPath("$[*].id", contains("a", "b")));
    }

    @Test
    void searchWithNoMatchesIsAnEmptyList() throws Exception {
        mvc.perform(get("/api/tracks").param("q", "polka"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "101"})
    void limitOutsideOneToHundredIs400(String limit) throws Exception {
        mvc.perform(get("/api/tracks").param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("limit must be between 1 and 100, was " + limit));
    }

    @Test
    void limitOfExactlyTheMaximumIsAllowed() throws Exception {
        mvc.perform(get("/api/tracks").param("limit", String.valueOf(TrackController.MAX_LIMIT)))
                .andExpect(status().isOk());
    }

    @Test
    void limitThatIsNotANumberIs400() throws Exception {
        mvc.perform(get("/api/tracks").param("limit", "lots"))
                .andExpect(status().isBadRequest());
    }

    // ______________ top ______________

    @Test
    void topListsTheMostPopularFirst() throws Exception {
        mvc.perform(get("/api/tracks/top").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].popularity", contains(90, 80, 70)));
    }

    @Test
    void topChecksTheLimitToo() throws Exception {
        mvc.perform(get("/api/tracks/top").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    // ______________ one track ______________

    @Test
    void byIdReturnsTheTracksDetails() throws Exception {
        mvc.perform(get("/api/tracks/c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("c"))
                .andExpect(jsonPath("$.title").value("Charlie"))
                .andExpect(jsonPath("$.artists", contains("Cat")))
                .andExpect(jsonPath("$.album").value("Album c"))
                .andExpect(jsonPath("$.genre").value("rock"))
                .andExpect(jsonPath("$.popularity").value(70))
                .andExpect(jsonPath("$.explicit").value(false))
                .andExpect(jsonPath("$.tempo").value(128.0))
                .andExpect(jsonPath("$.duration").value("4:00"));
    }

    @Test
    void unknownIdIs404WithAProblemDetail() throws Exception {
        mvc.perform(get("/api/tracks/zzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No track with id 'zzz'"));
    }
}
