# fundermentals: a music library and playlist engine

A Java 21 project that loads ~90,000 real Spotify tracks and builds a playlist engine on top of them,
using **hand-written data structures** instead of `java.util` wherever the structure is the point.

It is a portfolio project for core Java fundamentals: OOP, interfaces, generics, exceptions,
custom iterators, design patterns and dependency injection wired by hand.

> **Status:** in progress. The data structures, domain model, CSV loading, repository and playlists are done
> and tested. Undo/redo, playback and the REST API are being built next (see [Roadmap](#roadmap)).

---

## What it demonstrates

| Area | Where |
|---|---|
| **Hand-written data structures** | `DoublyLinkedList` (sentinel nodes), `LinkedPositionalList` (Position ADT), `LinkedStack`, `LinkedQueue` |
| **Custom iterators** | `FilteringIterator` and `RepeatingIterator` (decorators that stack), the iterators inside each list |
| **Inheritance and polymorphism** | `MediaItem` → `Track`, `PodcastEpisode`; `Playlist` → `SmartPlaylist` |
| **Interfaces with `default`/`static` methods** | `Playable`, `Searchable`, `Repository<T, ID>`, `DataSetLoader<T>` |
| **Generics, including PECS** | `Repository<T, ID>`, `Iterator<? extends E>`, `Predicate<? super E>`, `Comparator<? super Track>` |
| **Checked vs unchecked exceptions** | `MalformedRecordException` (checked), `TrackNotFoundException`, `EmptyCollectionException` |
| **Design patterns** | Builder (`Track.Builder`), Template Method (`checkCanAdd` hook), Decorator (iterators), Command (undo/redo), Strategy (shuffle) |
| **Logging** | SLF4J with parameterised messages |
| **Testing** | 300+ JUnit 5 tests: parameterised tests, `@TempDir`, log capture, real-dataset tests |

Spring Boot is used **only as a thin web layer**. Every other class is a plain Java object with no Spring
annotations, wired together by hand with constructor and setter injection.

---

## Getting started

### 1. Prerequisites

- **JDK 21** or newer.
- **Maven is not needed:** the project ships with the Maven wrapper (`./mvnw`).

### 2. Download the dataset

The dataset is **not** in this repository (it's 20 MB and gitignored). The code expects it at:

```
data/spotify_tracks.csv
```

**Option A: command line** (from the project root)

```bash
mkdir -p data
curl -L -o data/spotify_tracks.csv \
  https://huggingface.co/datasets/maharshipandya/spotify-tracks-dataset/resolve/main/dataset.csv
```

**Option B: by hand**

1. Download `dataset.csv` from the
   [Spotify Tracks Dataset on Hugging Face](https://huggingface.co/datasets/maharshipandya/spotify-tracks-dataset)
   (*Files and versions* tab).
2. Create a folder called `data` in the project root, next to `pom.xml`.
3. Move the file into it and rename it to **`spotify_tracks.csv`**.

**Check it:** the file should be about 20 MB with 114,001 lines (a header plus 114,000 rows):

```bash
wc -l data/spotify_tracks.csv     # 114001 data/spotify_tracks.csv
```

When loaded, it gives **89,740 tracks**. The rest are skipped on purpose, and the loader logs why:
24,259 rows repeat a track id that's already loaded (the same song listed under several genres),
and 1 row has a blank title.

The dataset was published by Maharshi Pandya (originally on Kaggle). See the dataset page for its terms of use.

### 3. Build and run the tests

```bash
./mvnw test
```

The tests use a small sample CSV in `src/test/resources`, so **they pass without the real dataset**.
A few extra tests run against `data/spotify_tracks.csv` when it's present, and are skipped otherwise.

### In IntelliJ IDEA

1. **File → Open** and choose the project folder (the one with `pom.xml`).
2. If imports show red, open the **Maven** tool window and click **Reload All Maven Projects**.
3. Right-click `src/test/java` and choose **Run 'All Tests'**.

---

## Project layout

```
src/main/java/com/khanyi/java/fundermentals/
├── datastructures/   linked list, positional list, stack, queue, custom iterators
├── model/            MediaItem, Track, PodcastEpisode, Playlist, SmartPlaylist
├── data/             CSV loader, Repository<T, ID>, in-memory track repository
└── service/          undo/redo commands (in progress)
```

---

## Roadmap

- [x] Hand-written data structures
- [x] Domain model (tracks, podcast episodes)
- [x] CSV loading with error handling
- [x] Generic repository with search, sorting and top-N
- [x] Custom iterators, playlists and rule-based smart playlists
- [ ] Undo/redo (Command pattern on two stacks)
- [ ] Playback: Play Next queue, recently played, shuffle strategies, repeat modes
- [ ] Hand-wired composition root (`PlaylistEngineAssembler`)
- [ ] REST API
