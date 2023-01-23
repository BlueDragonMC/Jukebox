# Jukebox
![GitHub last commit](https://img.shields.io/github/last-commit/BlueDragonMC/Jukebox)
[![.github/workflows/gradle.yml](https://github.com/BlueDragonMC/Jukebox/actions/workflows/gradle.yml/badge.svg)](https://github.com/BlueDragonMC/Jukebox/actions/workflows/gradle.yml)

[![BlueDragon Logo](./favicon_64.png)](https://bluedragonmc.com)

**A plugin and API for playing Note Block Studio (NBS) songs on a Velocity proxy.**

## Dependencies
This plugin requires [Protocolize](https://github.com/Exceptionflug/protocolize) for sound effects and inventories ([SpigotMC page](https://www.spigotmc.org/resources/protocolize-protocollib-for-bungeecord-waterfall-velocity.63778/)).

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
```kotlin
import com.bluedragonmc.jukebox.Song

val song = Song.load(Paths.get("path/to/nbs/file.nbs")) // or alternatively,
val song = Song.load("my_song.nbs") // passing in a String allows the song 
                                    // to be loaded from the "songs" subfolder
                                    // of the plugin's data directory.

val player = proxyServer.getPlayer("demo")
Song.play(song, player) // Starts the song
Song.pause(player) // Pauses the song
Song.resume(player) // Resumes the current song, if one is playing
Song.stop(player) // Stops and clears the current song

val nowPlaying: Song = Song.getCurrentSong(player) // Returns the song that the player 
                                                   // is currently listening to, or null
```

## Special Thanks
- Both the [OpenNBS format page](https://opennbs.org/nbs) and EmortalMC's [NBStom](https://github.com/EmortalMC/NBStom) project were great guides for writing the NBS parser for this project.