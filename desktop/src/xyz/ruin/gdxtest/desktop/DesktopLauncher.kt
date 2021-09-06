package xyz.ruin.gdxtest.desktop

import kotlin.jvm.JvmStatic
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import xyz.ruin.gdxtest.GDXTest

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        config.width = 1280;
        config.height = 720;
        LwjglApplication(GDXTest(), config)
    }
}