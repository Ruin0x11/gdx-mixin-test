package xyz.ruin.gdxtest.core

import com.badlogic.gdx.files.FileHandle
import java.nio.file.Path

interface IResourcePath {
    fun getFileHandle(): FileHandle
}