package com.khanyi.java.fundermentals;

import com.khanyi.java.fundermentals.web.PlayerController;
import com.khanyi.java.fundermentals.web.PlaylistController;
import com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the real Spring application (with the small sample CSV instead of the 20 MB dataset)
 * to check the wiring between Spring and the hand-built engine.
 */
@SpringBootTest(properties = "fundermentals.dataset=src/test/resources/data/spotify_sample.csv")
@AutoConfigureMockMvc
class FundermentalsApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mvc;

    @Test
    void contextLoads() {
        PlaylistEngineAssembler engine = context.getBean(PlaylistEngineAssembler.class);

        assertEquals(5, engine.trackRepository().count());
    }

    @Test
    void springKnowsTheAssemblerButNotTheEngineInsideIt() {
        assertEquals(1, context.getBeanNamesForType(PlaylistEngineAssembler.class).length);
        assertEquals(0, context.getBeanNamesForType(com.khanyi.java.fundermentals.service.PlaylistService.class).length);
        assertEquals(0, context.getBeanNamesForType(com.khanyi.java.fundermentals.service.PlayerService.class).length);
        assertNotNull(context.getBean(PlaylistController.class));
        assertNotNull(context.getBean(PlayerController.class));
    }

    @Test
    void servesTracksFromTheLoadedFile() throws Exception {
        mvc.perform(get("/api/tracks/t3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Too Good"));
    }

    @Test
    void engineErrorsComeBackAsProblemDetails() throws Exception {
        mvc.perform(get("/api/playlists/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No playlist with id 'nope'"));
    }

    @Test
    void springsOwnErrorsComeBackAsProblemDetailsToo() throws Exception {
        // spring.mvc.problemdetails.enabled=true in application.properties
        mvc.perform(post("/api/playlists").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }
}
