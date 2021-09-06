package xyz.ruin.gdxtest.core

import com.badlogic.gdx.Files
import com.badlogic.gdx.files.FileHandle
import java.io.File
import java.io.InputStream

class ModLocalFileHandle<T: IMod>(val path: ModLocalPath<T>) : FileHandle(path.subpath, Files.FileType.Classpath) {
    override fun read(): InputStream {
        val jarFile = path.instancedMod.jarFile
        return jarFile.getEntry(path.subpath).let { jarFile.getInputStream(it) }
    }
}