package xyz.ruin.gdxtest.core

import java.nio.file.Path

object PathUtils {
    fun modLocalPath(modId: String, suffix: String): String {
        return "$modId:$suffix"
    }
}