package xyz.ruin.gdxtest.core

import com.badlogic.gdx.files.FileHandle

class ModLocalPath<T: IMod>(val mod: Class<T>, val subpath: String) : IResourcePath {
    override fun getFileHandle(): FileHandle {
        return ModLocalFileHandle(this)
    }

    val instancedMod: InstancedMod = GDXTest.mods.getMod(mod)!!
}