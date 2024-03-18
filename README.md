# Jukebox

![GitHub last commit](https://img.shields.io/github/last-commit/BlueDragonMC/Jukebox)
[![.github/workflows/gradle.yml](https://github.com/BlueDragonMC/Jukebox/actions/workflows/gradle.yml/badge.svg)](https://github.com/BlueDragonMC/Jukebox/actions/workflows/gradle.yml)

[![BlueDragon Logo](./favicon_64.png)](https://bluedragonmc.com)

**A plugin and API for playing Note Block Studio (NBS) songs on a Velocity proxy.**

## Dependencies

This plugin requires [Protocolize](https://github.com/Exceptionflug/protocolize) for sound effects and
inventories ([SpigotMC page](https://www.spigotmc.org/resources/protocolize-protocollib-for-bungeecord-waterfall-velocity.63778/)).

## Usage as a Velocity Plugin

**Setup**:

1. Copy the plugin into your `plugins` folder
2. Launch the Velocity proxy
3. Place NBS files into the newly-created folder, located at: `plugins/bluedragon-jukebox/songs/`
4. Restart the Velocity proxy
5. Give yourself and other players the necessary permissions

**In-game usage**:

* Use `/play` to select a song. A menu will open, and clicking on a song will play it.
* Use `/pause` to pause the song, and `/resume` to resume it after pausing.

**Permissions**:

| Command   | Permission     |
|-----------|----------------|
| `/play`   | jukebox.play   |
| `/pause`  | jukebox.pause  |
| `/resume` | jukebox.resume |

## Usage as an API

Add the following (or similar) to your build file:

```kotlin
repositories {
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.github.BlueDragonMC:Jukebox:${version}")
}
```

Then, in your plugin class, inject the `JukeboxPlugin` instance into a variable:

```kotlin
import com.bluedragonmc.jukebox.JukeboxPlugin
import com.google.inject.Inject

// ...

@Inject
lateinit var jukeboxPlugin: JukeboxPlugin
```

With the `JukeboxPlugin` instance, you can get a `SongLoader` and a `SongPlayer`:

```kotlin
val songLoader = jukeboxPlugin.getSongLoader()

// Your SongPlayer instance will be isolated from other plugins; see the note below for more details.
val songPlayer = jukeboxPlugin.getSongPlayer(myPluginInstance, proxyServer)
```

Here is an example of what you can do with them:

```kotlin
val mySong = songLoader.load(Path("my_song.nbs").readBytes())
val player = proxyServer.getPlayer("YourUsername")

// Play the song
songPlayer.play(mySong, player, startTimeInTicks = 0)
// Temporarily stop the song
songPlayer.pause(player)
// Resume a paused song
songPlayer.resume(player)
// Clear the current song and stop playing it.
// This prevents the song from being resumed.
songPlayer.stop(player)
// Get some information about the current song
val info = songPlayer.getCurrentSong(player)
println("Paused: ${info.isPaused}")
println("Current song: ${info.song.songName}; arranged by ${info.song.author} and originally created by ${info.song.originalAuthor}. Description: \"${info.song.description}\"")
println("Progress: ${info.currentTick} / ${info.song.durationInTicks} ticks.")
```

### A note on class loader isolation
Velocity strictly isolates plugins by using a different class loader for each one.
This means that `SongPlayer` instances created via different plugins will be unaware of each other and could play songs to the same player at the same time.
To combat this, you will have to listen to the plugin's events (see below) and stop playing in your plugin if a song starts playing from another plugin.

## Events

| Event name      | Purpose                                                               |
|-----------------|-----------------------------------------------------------------------|
| SongEndEvent    | Called when a song ends or was manually stopped.                      |
| SongPauseEvent  | Called when a song is paused.                                         |
| SongResumeEvent | Called when a pause song is resumed.                                  |
| SongStartEvent  | Called once when a song is started, and not again when it is resumed. |

## Special Thanks

- Both the [OpenNBS format page](https://opennbs.org/nbs) and EmortalMC's [NBStom](https://github.com/EmortalMC/NBStom)
  project were great guides for writing the NBS parser for this project.