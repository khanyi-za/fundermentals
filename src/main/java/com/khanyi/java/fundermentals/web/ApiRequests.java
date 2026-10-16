package com.khanyi.java.fundermentals.web;

/**
 * The JSON bodies clients send. Each is a record: Jackson builds it from the JSON,
 * and it can't be changed after that.
 *
 * Grouped in one class because they're tiny; use them as ApiRequests.NewPlaylist etc.
 */
public final class ApiRequests {

    private ApiRequests() {
    }

    /** {"name": "Road trip"} */
    public record NewPlaylist(String name) {
    }

    /** {"name": "Jazz", "genre": "jazz", "limit": 25} */
    public record NewGenrePlaylist(String name, String genre, int limit) {
    }

    /** {"name": "Gym", "minBpm": 120, "maxBpm": 130, "limit": 20} */
    public record NewTempoPlaylist(String name, double minBpm, double maxBpm, int limit) {
    }

    /** {"name": "New name"} */
    public record Rename(String name) {
    }

    /** {"trackId": "abc"} adds at the end; {"trackId": "abc", "index": 0} adds at the top. */
    public record AddTrack(String trackId, Integer index) {
    }

    /** {"from": 3, "to": 0}: the item at 3 ends up at 0 */
    public record Move(int from, int to) {
    }

    /** {"trackId": "abc"} */
    public record QueueTrack(String trackId) {
    }

    /** {"strategy": "weighted"} */
    public record ShuffleChoice(String strategy) {
    }

    /** {"mode": "all"} */
    public record RepeatChoice(String mode) {
    }
}
