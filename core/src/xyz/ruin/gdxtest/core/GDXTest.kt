package xyz.ruin.gdxtest.core

import com.badlogic.gdx.Game
import java.io.File

class GDXTest : Game() {
    override fun create() {
        init()
        setScreen(TiledMapScreen(this))
    }

    private fun init() {
        val modInfos = listOf(
            ModInfo(File("C:\\Users\\yuno\\build\\gdxtest\\mod\\build\\libs\\mod-1.0.jar"), "mod.mixins.json"),
            ModInfo(File("C:\\Users\\yuno\\build\\gdxtest\\mod2\\build\\libs\\mod2-1.0.jar"), "mod2.mixins.json")
        )

        val scanner = ModScanner()
        modInfos.forEach { scanner.scanMod(it) }

        mods.mods.putAll(scanner.loadMods())
        mods.initMods()
    }

    override fun render() {
        super.render()
    }

    companion object {
        val mods: ModRepo = ModRepo(mutableMapOf())
    }
}