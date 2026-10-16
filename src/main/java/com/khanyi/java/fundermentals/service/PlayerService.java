package com.khanyi.java.fundermentals.service;

import com.khanyi.java.fundermentals.data.Repository;
import com.khanyi.java.fundermentals.datastructures.LinkedQueue;
import com.khanyi.java.fundermentals.datastructures.LinkedStack;
import com.khanyi.java.fundermentals.datastructures.Queue;
import com.khanyi.java.fundermentals.datastructures.RepeatingIterator;
import com.khanyi.java.fundermentals.datastructures.Stack;
import com.khanyi.java.fundermentals.model.MediaItem;
import com.khanyi.java.fundermentals.model.Playlist;
import com.khanyi.java.fundermentals.model.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * The music player: what's playing, what's next, and what was played before.
 *
 * Where the next item comes from, in order of priority:
 *   1. items you stepped back past with previous()  (the "forward" stack)
 *   2. the Play Next queue                          (a LinkedQueue: first in, first out)
 *   3. the playlist, in the order the ShuffleStrategy chose
 *
 * Dependencies:
 *   - constructor injection for what it can't work without (playlists, tracks)
 *   - setter injection for what can change while it runs (the shuffle strategy)
 */
public class PlayerService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private final PlaylistService playlists;
    private final Repository<Track, String> tracks;

    private ShuffleStrategy shuffleStrategy = new InOrderStrategy();
    private RepeatMode repeatMode = RepeatMode.OFF;

    private Playlist playlist;                                // null until play() is called
    private List<MediaItem> playOrder = List.of();            // the playlist, as ordered by the strategy
    private int nextIndex;                                    // where in playOrder the next item comes from
    private MediaItem current;                                // null when nothing is playing

    private final Queue<MediaItem> playNextQueue = new LinkedQueue<>();
    private final Stack<MediaItem> back = new LinkedStack<>();      // recently played, most recent on top
    private final Stack<MediaItem> forward = new LinkedStack<>();   // stepped back past, to replay first

    public PlayerService(PlaylistService playlists, Repository<Track, String> tracks) {
        if (playlists == null || tracks == null) {
            throw new IllegalArgumentException("playlists and tracks must not be null");
        }
        this.playlists = playlists;
        this.tracks = tracks;
    }

    // ______________ setter injection ______________

    /**
     * Swaps the shuffle strategy, even in the middle of a playlist. The current item keeps
     * playing, and the rest of the playlist is re-ordered by the new strategy.
     */
    public void setShuffleStrategy(ShuffleStrategy shuffleStrategy) {
        if (shuffleStrategy == null) {
            throw new IllegalArgumentException("shuffleStrategy must not be null");
        }
        this.shuffleStrategy = shuffleStrategy;
        if (playlist != null) {
            reorderAround(current);
        }
        log.info("Shuffle strategy is now {}", shuffleStrategy.name());
    }

    public ShuffleStrategy getShuffleStrategy() {
        return shuffleStrategy;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        if (repeatMode == null) {
            throw new IllegalArgumentException("repeatMode must not be null");
        }
        this.repeatMode = repeatMode;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    /** Like pressing the repeat button: OFF -> ALL -> ONE -> OFF. */
    public RepeatMode cycleRepeatMode() {
        repeatMode = repeatMode.next();
        return repeatMode;
    }

    // ______________ playing ______________

    /**
     * Starts a playlist from the top of a fresh play order. Later edits to the playlist
     * are picked up the next time it is played.
     *
     * @return the first item
     */
    public MediaItem play(String playlistId) {
        Playlist chosen = playlists.getPlaylist(playlistId);
        if (chosen.isEmpty()) {
            throw new IllegalArgumentException("Playlist '" + chosen.getName() + "' is empty");
        }
        playlist = chosen;
        playOrder = shuffleStrategy.order(chosen.getItems());
        clear(forward);
        if (current != null) {
            back.push(current);
        }
        current = playOrder.get(0);   // the playlist you chose starts now; queued tracks come after it
        nextIndex = 1;
        log.info("Playing '{}' ({} items, {}, repeat {})",
                chosen.getName(), playOrder.size(), shuffleStrategy.name(), repeatMode);
        return current;
    }

    /** The user pressed "next": always moves on, even with repeat ONE. */
    public Optional<MediaItem> next() {
        return advance();
    }

    /** The current item finished on its own: with repeat ONE it plays again, otherwise moves on. */
    public Optional<MediaItem> trackFinished() {
        if (repeatMode == RepeatMode.ONE && current != null) {
            back.push(current);   // it was played; it goes into the history again
            return Optional.of(current);
        }
        return advance();
    }

    /**
     * Goes back to the item played before this one. The item you were on is kept,
     * so next() returns to it.
     *
     * @throws IllegalStateException if nothing has been played before
     */
    public MediaItem previous() {
        if (back.isEmpty()) {
            throw new IllegalStateException("Nothing was played before this");
        }
        if (current != null) {
            forward.push(current);
        }
        current = back.pop();
        log.debug("Back to {}", current.getDisplayName());
        return current;
    }

    public Optional<MediaItem> nowPlaying() {
        return Optional.ofNullable(current);
    }

    public Optional<Playlist> currentPlaylist() {
        return Optional.ofNullable(playlist);
    }

    // ______________ Play Next ______________

    /** Puts a library track in the Play Next queue: it plays after the current item, before the playlist continues. */
    public void playNext(String trackId) {
        Track track = tracks.getById(trackId);
        playNextQueue.enqueue(track);
        log.debug("Queued {}", track.getDisplayName());
    }

    public int queueSize() {
        return playNextQueue.size();
    }

    // ______________ looking ahead and back ______________

    /**
     * The next items that would play, without changing anything.
     * With repeat ALL the playlist wraps round, so this can show more items than the playlist has.
     */
    public List<MediaItem> upNext(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative, was " + count);
        }
        List<MediaItem> result = new ArrayList<>(peekAll(forward));
        result.addAll(peekAll(playNextQueue));

        Iterator<MediaItem> rest;
        if (repeatMode == RepeatMode.ALL) {
            List<MediaItem> rotated = new ArrayList<>(playOrder.subList(nextIndex, playOrder.size()));
            rotated.addAll(playOrder.subList(0, nextIndex));
            rest = RepeatingIterator.forever(rotated);
        } else {
            rest = playOrder.subList(nextIndex, playOrder.size()).iterator();
        }
        while (result.size() < count && rest.hasNext()) {
            result.add(rest.next());
        }
        return new ArrayList<>(result.subList(0, Math.min(count, result.size())));
    }

    /** Everything played so far, most recent first. */
    public List<MediaItem> recentlyPlayed() {
        return peekAll(back);
    }

    // ______________ helpers ______________

    /** Moves the current item into the history and takes the next one. Empty when the end is reached. */
    private Optional<MediaItem> advance() {
        if (current != null) {
            back.push(current);
        }
        current = takeNext();
        if (current != null) {
            log.debug("Now playing {}", current.getDisplayName());
        }
        return Optional.ofNullable(current);
    }

    private MediaItem takeNext() {
        if (!forward.isEmpty()) {
            return forward.pop();
        }
        if (!playNextQueue.isEmpty()) {
            return playNextQueue.dequeue();
        }
        if (nextIndex == playOrder.size() && repeatMode == RepeatMode.ALL) {
            nextIndex = 0;   // wrap round to the start
        }
        if (nextIndex < playOrder.size()) {
            return playOrder.get(nextIndex++);
        }
        return null;         // the end of the playlist
    }

    /** New play order from the strategy, with the current item kept first so it isn't played twice in a row. */
    private void reorderAround(MediaItem playing) {
        List<MediaItem> rest = new ArrayList<>(playlist.getItems());
        boolean playingIsInPlaylist = playing != null && rest.remove(playing);
        List<MediaItem> order = new ArrayList<>();
        if (playingIsInPlaylist) {
            order.add(playing);
        }
        order.addAll(shuffleStrategy.order(rest));
        playOrder = order;
        nextIndex = playingIsInPlaylist ? 1 : 0;
    }

    /** Lists a stack top-first, using only push and pop, and leaves it exactly as it was. */
    private static List<MediaItem> peekAll(Stack<MediaItem> stack) {
        List<MediaItem> items = new ArrayList<>();
        Stack<MediaItem> temp = new LinkedStack<>();
        while (!stack.isEmpty()) {
            MediaItem item = stack.pop();
            items.add(item);
            temp.push(item);
        }
        while (!temp.isEmpty()) {
            stack.push(temp.pop());
        }
        return items;
    }

    /** Lists a queue front-first, using only enqueue and dequeue: each item goes round once. */
    private static List<MediaItem> peekAll(Queue<MediaItem> queue) {
        List<MediaItem> items = new ArrayList<>();
        for (int i = queue.size(); i > 0; i--) {
            MediaItem item = queue.dequeue();
            items.add(item);
            queue.enqueue(item);
        }
        return items;
    }

    private static void clear(Stack<MediaItem> stack) {
        while (!stack.isEmpty()) {
            stack.pop();
        }
    }
}
