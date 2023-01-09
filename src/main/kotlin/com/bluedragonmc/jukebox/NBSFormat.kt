package com.bluedragonmc.jukebox

enum class NBSFormat {
    CLASSIC, MODERN_1, MODERN_2, MODERN_3, MODERN_4, MODERN_5;

    companion object {
        fun modern(version: Byte) = when (version.toInt()) {
            1 -> MODERN_1
            2 -> MODERN_2
            3 -> MODERN_3
            4 -> MODERN_4
            5 -> MODERN_5
            else -> error("Invalid version byte! ($version)")
        }
    }
}