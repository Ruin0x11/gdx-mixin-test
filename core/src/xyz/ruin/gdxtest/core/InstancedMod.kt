package xyz.ruin.gdxtest.core

import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

class InstancedMod(val modInfo: ModInfo, val impl: IMod) {
    val jarFile: JarFile
        get() = JarFile(modInfo.jarPath)

    val classLoader: URLClassLoader
        get() {
            val url = URL("file", null, jarFile.name)
            return URLClassLoader(arrayOf(url))
        }
}
