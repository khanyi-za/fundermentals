package com.khanyi.java.fundermentals.web;

import com.khanyi.java.fundermentals.data.MalformedRecordException;
import com.khanyi.java.fundermentals.wiring.PlaylistEngineAssembler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The bridge between Spring and the hand-wired engine.
 *
 * Spring gets ONE bean, the assembler. Everything inside it (repository, services,
 * strategies) was built by PlaylistEngineAssembler with plain `new`, so Spring
 * never sees those classes and none of them needs a Spring annotation.
 */
@Configuration
public class EngineConfig {

    /**
     * Loads the dataset once at startup. The path comes from application.properties
     * (fundermentals.dataset), so tests can point it at a small sample file.
     */
    @Bean
    public PlaylistEngineAssembler playlistEngine(@Value("${fundermentals.dataset}") Path dataset) {
        try {
            return PlaylistEngineAssembler.fromSpotifyCsv(dataset);
        } catch (IOException | MalformedRecordException e) {
            throw new IllegalStateException("Could not load the dataset from " + dataset.toAbsolutePath()
                    + ". Download it as the README explains, or set fundermentals.dataset", e);
        }
    }
}
