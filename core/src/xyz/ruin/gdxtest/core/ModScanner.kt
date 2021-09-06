package xyz.ruin.gdxtest.core

import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.spongepowered.asm.mixin.Mixins
import xyz.ruin.gdxtest.core.loader.JarLoader
import java.net.URL

class ModScanner {
    fun scanMod(modInfo: ModInfo) {
        scannedMods.add(modInfo)
        Mixins.addConfiguration(modInfo.mixin)
        JarLoader.addToClassPath(modInfo.jarPath)
    }

    fun loadMods(): Map<String, InstancedMod> {
        val result: MutableMap<String, InstancedMod> = mutableMapOf()
        val mods: MutableMap<String, InstancedMod> = scannedMods.associateTo(result) {
            val url =  URL("jar:file:" + it.jarPath.absolutePath + "!/")
            val conf = ConfigurationBuilder().addClassLoader(GDXTest::class.java.classLoader).addUrls(url)
            val mods = Reflections(conf).getTypesAnnotatedWith(ModEntry::class.java)
            if (mods.size != 1) {
                throw IllegalArgumentException("Exactly one ModEntry class must be annotated per mod")
            }
            val modEntry = mods.first()
            val modMetadata = modEntry.getAnnotation(ModEntry::class.java)!!
            val modImpl = modEntry.getConstructor().newInstance() as IMod
            println("Load mod ${modImpl.name}")
            modMetadata.id to InstancedMod(it, modImpl)
        }
        return mods
    }

    private val scannedMods: MutableList<ModInfo> = mutableListOf()
}