package xyz.ruin.gdxtest.launcher

import java.io.File
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.service.IMixinService
import java.util.*

class MyTweaker : ITweaker {
    init {
        val start = MixinBootstrap::class.java.getDeclaredMethod("start")
        start.isAccessible = true
        start.invoke(null)
    }

    override fun acceptOptions(args: List<String>, gameDir: File, assetsDir: File, profile: String) {
        val doInit = MixinBootstrap::class.java.getDeclaredMethod("doInit", List::class.java)
        doInit.isAccessible = true
        doInit.invoke(null, args.toList())
    }

    override fun injectIntoClassLoader(classLoader: MyClassLoader) {
        val inject = MixinBootstrap::class.java.getDeclaredMethod("inject")
        inject.isAccessible = true
        inject.invoke(null)
    }

    override val launchTarget: String = "xyz.ruin.gdxtest.desktop.DesktopLauncher"
    override val launchArguments: Array<String> = arrayOf()
}
