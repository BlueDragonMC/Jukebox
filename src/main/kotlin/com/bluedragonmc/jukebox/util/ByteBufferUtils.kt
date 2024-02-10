package com.bluedragonmc.jukebox.util

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

/*
This file was created from EmortalMC/NBStom under the MIT License on 2023/01/07.
https://github.com/emortalmc/NBStom/blob/main/src/main/kotlin/dev/emortal/nbstom/ByteBufferUtils.kt
 */

val ByteBuffer.string: String
    get() {
        val length = this.int
        if (length == 0) return ""
        val bytes = ByteArray(length)
        this.get(bytes)
        val byteBuffer = ByteBuffer.wrap(bytes)
        val charBuffer: CharBuffer = StandardCharsets.UTF_8.decode(byteBuffer)
        val string = CharArray(length)
        charBuffer.get(string)
        return String(string)
    }

val ByteBuffer.unsignedShort: Int
    get() {
        val bytes = ByteArray(2)
        this[bytes]
        var integerVal = 0
        for (i in bytes.indices.reversed()) {
            val b = bytes[i]
            integerVal += java.lang.Byte.toUnsignedInt(b) shl i * 8
        }
        return integerVal
    }

val ByteBuffer.byte: Byte get() = get()