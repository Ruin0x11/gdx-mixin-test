package xyz.ruin.gdxtest.launcher

import java.io.File

interface ITweaker {
    fun acceptOptions(args: List<String>, gameDir: File, assetsDir: File, profile: String)
    fun injectIntoClassLoader(classLoader: MyClassLoader)
    val launchTarget: String
    val launchArguments: Array<String>
}
