package com.khanyi.java.fundermentals.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RepeatModeTest {

    @Test
    void nextCyclesLikeARepeatButton() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next());
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next());
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next());
    }

    @Test
    void threeClicksComeBackToTheStart() {
        for (RepeatMode mode : RepeatMode.values()) {
            assertEquals(mode, mode.next().next().next());
        }
    }

    @Test
    void valueOfTurnsTextIntoAMode() {
        // what the REST API will use to read "ALL" from a request
        assertEquals(RepeatMode.ALL, RepeatMode.valueOf("ALL"));
        assertThrows(IllegalArgumentException.class, () -> RepeatMode.valueOf("ALLL"));
    }

    @Test
    void valuesReturnsAFreshCopyEachTime() {
        // why RepeatMode caches VALUES instead of calling values() in next()
        RepeatMode[] first = RepeatMode.values();
        first[0] = null;

        assertEquals(RepeatMode.OFF, RepeatMode.values()[0]);
        assertNotSame(RepeatMode.values(), RepeatMode.values());
    }
}
