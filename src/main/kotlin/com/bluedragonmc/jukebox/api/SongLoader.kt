package com.bluedragonmc.jukebox.api

interface SongLoader {
    fun load(source: String?, bytes: ByteArray): Song
    fun load(bytes: ByteArray): Song = load(null, bytes)
}