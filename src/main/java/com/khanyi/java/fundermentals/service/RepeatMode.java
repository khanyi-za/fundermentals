package com.khanyi.java.fundermentals.service;

/** What the player does when it reaches the end of a track or of the playlist. */
public enum RepeatMode {

    /** Play the playlist once, then stop. */
    OFF,

    /** When the playlist ends, start it again from the top. */
    ALL,

    /** Play the current track again and again. */
    ONE;

    private static final RepeatMode[] VALUES = values();

    /** The next mode, the way a repeat button cycles: OFF -> ALL -> ONE -> OFF. */
    public RepeatMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
