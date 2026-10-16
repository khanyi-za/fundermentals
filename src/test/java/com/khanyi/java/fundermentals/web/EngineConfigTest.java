package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class EngineConfigTest {

    private final EngineConfig config = new EngineConfig();

    @Test
    void buildsTheEngineFromTheDatasetFile() throws Exception {
        Path sample = Path.of(getClass().getResource("/data/spotify_sample.csv").toURI());

        PlaylistEngineAssembler engine = config.playlistEngine(sample);

        assertEquals(5, engine.trackRepository().count());
    }

    @Test
    void aMissingDatasetStopsStartupWithAHelpfulMessage(@TempDir Path dir) {
        Path missing = dir.resolve("spotify_tracks.csv");

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> config.playlistEngine(missing));

        assertTrue(e.getMessage().startsWith("Could not load the dataset from " + missing.toAbsolutePath()));
        assertTrue(e.getMessage().contains("README"));
        assertInstanceOf(IOException.class, e.getCause());
    }
}
